package dev.cavatz.keybindertv.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dev.cavatz.keybindertv.KeyBinderTvApp
import dev.cavatz.keybindertv.data.model.ActionTarget
import dev.cavatz.keybindertv.data.model.ButtonIdentifier
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.model.KeyMapping
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.util.LongPressDetector

/**
 * Servicio de accesibilidad que intercepta pulsaciones y ejecuta los remapeos.
 */
class KeyInterceptorService : AccessibilityService() {

    companion object {
        private const val TAG: String = "KeyInterceptorService"
        private const val DEDUP_WINDOW_MS: Long = 2000L

        var instance: KeyInterceptorService? = null
            private set
    }

    var learningCallback: ((ButtonIdentifier) -> Unit)? = null
        private set

    private lateinit var repository: MappingRepository
    private lateinit var remapExecutor: RemapExecutor

    private data class DetectorEntry(val mapping: KeyMapping, val thresholdMs: Long, val detector: LongPressDetector)

    private val longPressDetectors: MutableMap<Int, DetectorEntry> = mutableMapOf()
    private val recentlyHandledKeys: MutableMap<String, Long> = mutableMapOf()

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app: KeyBinderTvApp = application as KeyBinderTvApp
        repository = app.mappingRepository
        remapExecutor = RemapExecutor(applicationContext)
        instance = this
        Log.i(TAG, "Servicio de accesibilidad conectado")
    }

    override fun onDestroy() {
        instance = null
        longPressDetectors.values.forEach { it.detector.cancel() }
        longPressDetectors.clear()
        Log.i(TAG, "Servicio de accesibilidad detenido")
        super.onDestroy()
    }

    override fun onInterrupt() {
        Log.w(TAG, "Servicio de accesibilidad interrumpido")
    }

    fun startLearningMode(callback: (ButtonIdentifier) -> Unit) {
        learningCallback = callback
        Log.d(TAG, "Modo aprendizaje activado")
    }

    fun stopLearningMode() {
        learningCallback = null
        Log.d(TAG, "Modo aprendizaje desactivado")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (learningCallback != null &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {
            val identifier = ButtonIdentifier(
                keyCode = event.keyCode,
                userLabel = KeyEvent.keyCodeToString(event.keyCode)
            )
            learningCallback?.invoke(identifier)
            stopLearningMode()
            return true
        }

        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) {
            return false
        }

        val config: KeyBinderConfig = repository.getConfigSync()
        val mapping: KeyMapping = config.mappings.firstOrNull { m ->
            m.isEnabled && m.button.keyCode == event.keyCode
        } ?: return false

        // Si ambas pulsaciones son de fábrica, no interceptar: Android/TCL
        // conserva íntegramente el comportamiento nativo del botón.
        if (mapping.shortPressUseDefault && mapping.longPressUseDefault) return false

        // Un mapping vacío no debe tragarse eventos del mando.
        if (mapping.shortPressAction == null && mapping.longPressAction == null &&
            !mapping.shortPressUseDefault && !mapping.longPressUseDefault
        ) return false

        /*
         * No deduplicar ACTION_DOWN y ACTION_UP por separado: ambos forman una
         * única pulsación y LongPressDetector debe consumir los dos. La
         * deduplicación anterior marcaba ACTION_UP como duplicado del ACTION_DOWN,
         * devolvía false y dejaba que Android/TCL ejecutase la acción original
         * del botón (por ejemplo, abrir Netflix).
         *
         * LongPressDetector ya controla repeatCount y el estado de cada pulsación.
         */
        return dispatchToLongPressDetector(event, mapping, config)
    }

    private fun dispatchToLongPressDetector(
        event: KeyEvent,
        mapping: KeyMapping,
        config: KeyBinderConfig
    ): Boolean {
        val current = longPressDetectors[event.keyCode]
        val entry = if (current == null ||
            current.mapping != mapping ||
            current.thresholdMs != config.longPressThresholdMs
        ) {
            current?.detector?.cancel()
            DetectorEntry(
                mapping = mapping,
                thresholdMs = config.longPressThresholdMs,
                detector = buildDetector(mapping, config)
            ).also { longPressDetectors[event.keyCode] = it }
        } else {
            current
        }
        return entry.detector.onKeyEvent(event)
    }

    private fun buildDetector(mapping: KeyMapping, config: KeyBinderConfig): LongPressDetector {
        return LongPressDetector(
            thresholdMs = config.longPressThresholdMs,
            onShortPress = {
                if (mapping.shortPressUseDefault) {
                    launchOriginalAction(mapping.button, config.showOverlayNotification)
                } else {
                    val target = mapping.shortPressAction ?: return@LongPressDetector
                    remapExecutor.launch(target, config.showOverlayNotification)
                }
            },
            onLongPress = {
                if (mapping.longPressUseDefault) {
                    launchOriginalAction(mapping.button, config.showOverlayNotification)
                } else {
                    // Mantener el comportamiento de las configuraciones antiguas:
                    // si no había acción larga definida, se repetía la acción corta.
                    val target = mapping.longPressAction ?: mapping.shortPressAction
                        ?: return@LongPressDetector
                    remapExecutor.launch(target, config.showOverlayNotification)
                }
            }
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName: String = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        if (learningCallback != null) {
            val identifier = ButtonIdentifier(
                sourcePackage = packageName,
                userLabel = packageName
            )
            learningCallback?.invoke(identifier)
            stopLearningMode()
            return
        }

        val config: KeyBinderConfig = repository.getConfigSync()
        val mapping: KeyMapping = config.mappings.firstOrNull { m ->
            m.isEnabled && m.button.sourcePackage == packageName
        } ?: return

        val dedupKey: String = "package_$packageName"
        if (isDuplicate(dedupKey)) return

        // En este flujo la app original ya se ha abierto: si la pulsación corta
        // está configurada como "De fábrica", no hay nada que sustituir.
        if (mapping.shortPressUseDefault) return
        val target = mapping.shortPressAction ?: return
        remapExecutor.launchAndKillSource(target, packageName, config.showOverlayNotification)
    }

    /**
     * Reproduce una acción original solo cuando el botón está identificado.
     * En el TCL probado, el código 4062 corresponde a Netflix.
     */
    private fun launchOriginalAction(button: ButtonIdentifier, showOverlay: Boolean) {
        val target = when (button.keyCode) {
            4062 -> ActionTarget("com.netflix.ninja", "Netflix")
            4065 -> ActionTarget("com.google.android.youtube.tv", "YouTube")
            4099 -> ActionTarget("com.amazon.amazonvideo.livingroom", "Prime Video")
            else -> if (!button.sourcePackage.isNullOrBlank()) {
                ActionTarget(button.sourcePackage, button.sourcePackage)
            } else {
                null
            }
        }

        if (target == null) {
            Log.w(TAG, "No se puede reproducir la acción de fábrica: botón no identificado (${button.keyCode})")
            return
        }
        remapExecutor.launch(target, showOverlay)
    }

    private fun isDuplicate(key: String): Boolean {
        val now: Long = System.currentTimeMillis()
        val lastHandled: Long? = recentlyHandledKeys[key]

        if (lastHandled != null && now - lastHandled < DEDUP_WINDOW_MS) {
            Log.d(TAG, "Evento duplicado ignorado: $key")
            return true
        }

        recentlyHandledKeys[key] = now
        recentlyHandledKeys.entries.removeIf { now - it.value > DEDUP_WINDOW_MS * 2 }
        return false
    }
}
