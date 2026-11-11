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
 * Intercepts physical remote-control keys and app launches on Google TV.
 *
 * ACTION_UP is always delivered to LongPressDetector. Deduplication is applied
 * only after a target has actually been launched, never to raw key events.
 */
class KeyInterceptorService : AccessibilityService() {

    companion object {
        private const val TAG = "KeyInterceptorService"
        private const val DEDUP_WINDOW_MS = 2000L

        var instance: KeyInterceptorService? = null
            private set
    }

    var learningCallback: ((ButtonIdentifier) -> Unit)? = null
        private set

    private lateinit var repository: MappingRepository
    private lateinit var remapExecutor: RemapExecutor

    private data class DetectorEntry(
        val mapping: KeyMapping,
        val thresholdMs: Long,
        val showOverlay: Boolean,
        val detector: LongPressDetector
    )

    private val longPressDetectors = mutableMapOf<Int, DetectorEntry>()
    private val recentlyHandledTargets = mutableMapOf<String, Long>()
    private var lastForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as KeyBinderTvApp
        repository = app.mappingRepository
        remapExecutor = RemapExecutor(applicationContext)
        instance = this
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onDestroy() {
        instance = null
        longPressDetectors.values.forEach { it.detector.cancel() }
        longPressDetectors.clear()
        recentlyHandledTargets.clear()
        super.onDestroy()
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    fun startLearningMode(callback: (ButtonIdentifier) -> Unit) {
        learningCallback = callback
    }

    fun stopLearningMode() {
        learningCallback = null
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (learningCallback != null &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {
            learningCallback?.invoke(
                ButtonIdentifier(
                    keyCode = event.keyCode,
                    userLabel = KeyEvent.keyCodeToString(event.keyCode)
                )
            )
            stopLearningMode()
            return true
        }

        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) {
            return false
        }

        val config = repository.getConfigSync()
        val mapping = config.mappings.firstOrNull { it.isEnabled && it.button.keyCode == event.keyCode }

        if (mapping == null) {
            longPressDetectors.remove(event.keyCode)?.detector?.cancel()
            return false
        }

        return dispatchToLongPressDetector(event, mapping, config)
    }

    private fun dispatchToLongPressDetector(
        event: KeyEvent,
        mapping: KeyMapping,
        config: KeyBinderConfig
    ): Boolean {
        var entry = longPressDetectors[event.keyCode]
        if (entry == null ||
            entry.mapping != mapping ||
            entry.thresholdMs != config.longPressThresholdMs ||
            entry.showOverlay != config.showOverlayNotification
        ) {
            entry?.detector?.cancel()
            entry = DetectorEntry(
                mapping = mapping,
                thresholdMs = config.longPressThresholdMs,
                showOverlay = config.showOverlayNotification,
                detector = buildDetector(mapping, config)
            )
            longPressDetectors[event.keyCode] = entry
        }
        return entry.detector.onKeyEvent(event)
    }

    private fun buildDetector(mapping: KeyMapping, config: KeyBinderConfig): LongPressDetector {
        return LongPressDetector(
            thresholdMs = config.longPressThresholdMs,
            onShortPress = {
                mapping.shortPressAction?.let { launchAndRemember(it, config.showOverlayNotification) }
            },
            onLongPress = {
                val target = mapping.longPressAction ?: mapping.shortPressAction
                target?.let { launchAndRemember(it, config.showOverlayNotification) }
            }
        )
    }

    private fun launchAndRemember(target: ActionTarget, showOverlay: Boolean) {
        if (remapExecutor.launch(target, showOverlay)) {
            rememberTarget(target.packageName)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == lastForegroundPackage) return
        lastForegroundPackage = packageName

        if (packageName == applicationContext.packageName) return

        if (learningCallback != null) {
            learningCallback?.invoke(
                ButtonIdentifier(sourcePackage = packageName, userLabel = packageName)
            )
            stopLearningMode()
            return
        }

        val config = repository.getConfigSync()
        val mapping = config.mappings.firstOrNull {
            it.isEnabled && it.button.sourcePackage == packageName
        } ?: return

        val target = mapping.shortPressAction ?: return

        // Never relaunch an app onto itself or create a source/target loop.
        if (target.packageName == packageName) return

        // A KeyEvent mapping may already have launched this target for the same key.
        if (wasRecentlyHandled(target.packageName)) return

        if (remapExecutor.launchAndKillSource(target, packageName, config.showOverlayNotification)) {
            rememberTarget(target.packageName)
        }
    }

    private fun rememberTarget(packageName: String) {
        val now = System.currentTimeMillis()
        recentlyHandledTargets[packageName] = now
        recentlyHandledTargets.entries.removeIf { now - it.value > DEDUP_WINDOW_MS * 2 }
    }

    private fun wasRecentlyHandled(packageName: String): Boolean {
        val now = System.currentTimeMillis()
        val last = recentlyHandledTargets[packageName] ?: return false
        if (now - last < DEDUP_WINDOW_MS) return true
        recentlyHandledTargets.remove(packageName)
        return false
    }
}
