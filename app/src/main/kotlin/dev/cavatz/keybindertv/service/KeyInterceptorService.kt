package dev.cavatz.keybindertv.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dev.cavatz.keybindertv.KeyBinderTvApp
import dev.cavatz.keybindertv.data.model.ButtonIdentifier
import dev.cavatz.keybindertv.data.model.KeyBinderConfig
import dev.cavatz.keybindertv.data.model.KeyMapping
import dev.cavatz.keybindertv.data.repository.MappingRepository
import dev.cavatz.keybindertv.util.LongPressDetector

/**
 * Service d'accessibilité — cœur de KeyBinderTv.
 *
 * Deux flux d'interception :
 * - Flux A (onKeyEvent) : boutons qui génèrent des KeyEvents → consommation directe
 * - Flux B (onAccessibilityEvent) : apps lancées directement → remplacement par-dessus
 *
 * Mode apprentissage :
 * - [startLearningMode] active la capture du prochain bouton
 * - [stopLearningMode] désactive
 *
 * Singleton via [instance] pour communication avec l'UI.
 */
class KeyInterceptorService : AccessibilityService() {

    companion object {
        private const val TAG: String = "KeyInterceptorService"

        /** Fenêtre de déduplication KeyEvent + Intent en millisecondes */
        private const val DEDUP_WINDOW_MS: Long = 2000L

        /** Référence statique pour communication avec l'UI (nullable si service inactif) */
        var instance: KeyInterceptorService? = null
            private set
    }

    /** Callback de mode apprentissage — appelé quand un bouton est détecté */
    var learningCallback: ((ButtonIdentifier) -> Unit)? = null
        private set

    private lateinit var repository: MappingRepository
    private lateinit var remapExecutor: RemapExecutor

    /**
     * Table des détecteurs long press actifs, indexés par keyCode.
     * Un détecteur est créé à la première réception d'un keyCode et réutilisé ensuite.
     */
    private val longPressDetectors: MutableMap<Int, LongPressDetector> = mutableMapOf()

    /**
     * Horodatages des derniers événements traités, pour déduplication KeyEvent/Intent.
     * Clé : "keycode_<code>" ou "package_<packageName>".
     */
    private val recentlyHandledKeys: MutableMap<String, Long> = mutableMapOf()

    // -------------------------------------------------------------------------
    // Cycle de vie du service
    // -------------------------------------------------------------------------

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app: KeyBinderTvApp = application as KeyBinderTvApp
        repository = app.mappingRepository
        remapExecutor = RemapExecutor(applicationContext)
        instance = this
        Log.i(TAG, "Service d'accessibilité connecté")
    }

    override fun onDestroy() {
        instance = null
        longPressDetectors.values.forEach { it.cancel() }
        longPressDetectors.clear()
        Log.i(TAG, "Service d'accessibilité détruit")
        super.onDestroy()
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service d'accessibilité interrompu")
    }

    // -------------------------------------------------------------------------
    // Mode apprentissage
    // -------------------------------------------------------------------------

    /** Active le mode apprentissage. Le prochain bouton détecté invoquera [callback]. */
    fun startLearningMode(callback: (ButtonIdentifier) -> Unit) {
        learningCallback = callback
        Log.d(TAG, "Mode apprentissage activé")
    }

    /** Désactive le mode apprentissage sans invoquer de callback. */
    fun stopLearningMode() {
        learningCallback = null
        Log.d(TAG, "Mode apprentissage désactivé")
    }

    // -------------------------------------------------------------------------
    // Flux A — interception par KeyEvent
    // -------------------------------------------------------------------------

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Mode apprentissage : capturer le premier keyCode reçu
        if (learningCallback != null
            && event.action == KeyEvent.ACTION_DOWN
            && event.repeatCount == 0
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

        // Ne pas dédupliquer les KeyEvents : ACTION_UP doit parvenir au détecteur.
        // Le filtre précédent rejetait aussi les nouvelles pressions de la même touche
        // pendant 2 secondes, ce qui perturbait les appuis courts et répétés.
        return dispatchToLongPressDetector(event, mapping, config)
    }

    /**
     * Crée ou réutilise un [LongPressDetector] pour le keyCode donné,
     * puis lui délègue l'événement.
     */
    private fun dispatchToLongPressDetector(
        event: KeyEvent,
        mapping: KeyMapping,
        config: KeyBinderConfig
    ): Boolean {
        val detector: LongPressDetector = if (
            event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0
        ) {
            // Recréer le détecteur à chaque nouvel appui pour prendre en compte
            // immédiatement toute modification du mapping ou des paramètres.
            longPressDetectors.remove(event.keyCode)?.cancel()
            buildDetector(mapping, config).also { longPressDetectors[event.keyCode] = it }
        } else {
            longPressDetectors[event.keyCode] ?: return false
        }

        val handled = detector.onKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP) {
            longPressDetectors.remove(event.keyCode)
        }
        return handled
    }

    /** Construit un [LongPressDetector] configuré pour un mapping donné. */
    private fun buildDetector(mapping: KeyMapping, config: KeyBinderConfig): LongPressDetector {
        return LongPressDetector(
            thresholdMs = config.longPressThresholdMs,
            onShortPress = {
                val target = mapping.shortPressAction ?: return@LongPressDetector
                remapExecutor.launch(target, config.showOverlayNotification)
            },
            onLongPress = {
                val target = mapping.longPressAction ?: mapping.shortPressAction
                    ?: return@LongPressDetector
                remapExecutor.launch(target, config.showOverlayNotification)
            }
        )
    }

    // -------------------------------------------------------------------------
    // Flux B — interception par changement de fenêtre (Intent direct)
    // -------------------------------------------------------------------------

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName: String = event.packageName?.toString() ?: return

        // Ignorer les événements provenant de notre propre application
        if (packageName == applicationContext.packageName) return

        // Mode apprentissage : capturer le package source
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

        // Flux B : toujours short press — l'app source s'est déjà ouverte, on la remplace
        val target = mapping.shortPressAction ?: return
        remapExecutor.launchAndKillSource(target, packageName, config.showOverlayNotification)
    }

    // -------------------------------------------------------------------------
    // Déduplication
    // -------------------------------------------------------------------------

    /**
     * Vérifie si la clé [key] a déjà été traitée dans la fenêtre [DEDUP_WINDOW_MS].
     * Enregistre la clé si elle est nouvelle. Nettoie les entrées expirées.
     */
    private fun isDuplicate(key: String): Boolean {
        val now: Long = System.currentTimeMillis()
        val lastHandled: Long? = recentlyHandledKeys[key]

        if (lastHandled != null && now - lastHandled < DEDUP_WINDOW_MS) {
            Log.d(TAG, "Événement dupliqué ignoré : $key")
            return true
        }

        recentlyHandledKeys[key] = now
        // Nettoyage des entrées expirées pour éviter une fuite mémoire
        recentlyHandledKeys.entries.removeIf { now - it.value > DEDUP_WINDOW_MS * 2 }
        return false
    }
}
