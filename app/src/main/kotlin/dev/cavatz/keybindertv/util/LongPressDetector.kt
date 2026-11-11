package dev.cavatz.keybindertv.util

import android.os.Handler
import android.os.Looper

/**
 * Abstracción mínima para programar tareas diferidas y poder probar el detector
 * sin depender de un Looper Android real.
 */
internal interface DelayedTaskScheduler {
    fun postDelayed(task: Runnable, delayMillis: Long)
    fun removeCallbacks(task: Runnable)
}

private class MainThreadDelayedTaskScheduler : DelayedTaskScheduler {
    private val handler = Handler(Looper.getMainLooper())

    override fun postDelayed(task: Runnable, delayMillis: Long) {
        handler.postDelayed(task, delayMillis)
    }

    override fun removeCallbacks(task: Runnable) {
        handler.removeCallbacks(task)
    }
}

/**
 * Detecta pulsaciones cortas y largas.
 *
 * El timeout de seguridad se sitúa, por defecto, un segundo después del umbral
 * configurado. Así no puede disparar una pulsación corta antes de un umbral largo
 * de hasta dos segundos.
 */
class LongPressDetector(
    private val thresholdMs: Long = DEFAULT_THRESHOLD_MS,
    private val safetyTimeoutMs: Long = thresholdMs + SAFETY_TIMEOUT_MS,
    private val scheduler: DelayedTaskScheduler = MainThreadDelayedTaskScheduler(),
    private val onShortPress: () -> Unit,
    private val onLongPress: () -> Unit
) {

    companion object {
        const val DEFAULT_THRESHOLD_MS: Long = 500L
        /** Margen adicional después del umbral de pulsación larga. */
        const val SAFETY_TIMEOUT_MS: Long = 1000L
    }

    private var isPending = false

    private val longPressRunnable = Runnable {
        if (isPending) {
            isPending = false
            scheduler.removeCallbacks(safetyRunnable)
            onLongPress()
        }
    }

    private val safetyRunnable = Runnable {
        if (isPending) {
            isPending = false
            scheduler.removeCallbacks(longPressRunnable)
            onShortPress()
        }
    }

    /** Procesa ACTION_DOWN. Los eventos repetidos se consumen solo si hay una pulsación pendiente. */
    fun onKeyDown(repeatCount: Int = 0): Boolean {
        if (repeatCount > 0) return isPending

        if (!isPending) {
            isPending = true
            scheduler.postDelayed(longPressRunnable, thresholdMs)
            scheduler.postDelayed(safetyRunnable, safetyTimeoutMs)
        }
        return true
    }

    /** Procesa ACTION_UP y ejecuta la acción corta si el umbral largo no venció. */
    fun onKeyUp(): Boolean {
        if (!isPending) return false

        isPending = false
        scheduler.removeCallbacks(longPressRunnable)
        scheduler.removeCallbacks(safetyRunnable)
        onShortPress()
        return true
    }

    /** Cancela los temporizadores pendientes sin ejecutar ninguna acción. */
    fun cancel() {
        isPending = false
        scheduler.removeCallbacks(longPressRunnable)
        scheduler.removeCallbacks(safetyRunnable)
    }
}
