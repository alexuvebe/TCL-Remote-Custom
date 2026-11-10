package dev.cavatz.keybindertv.util

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

/**
 * Détecte les pressions courtes et longues sur un KeyEvent.
 *
 * Logique :
 * - ACTION_DOWN (repeatCount=0) → démarre un timer de [thresholdMs] ms
 * - Timer expire → déclenche LONG press
 * - ACTION_UP avant timer → annule le timer, déclenche SHORT press
 * - Timeout de sécurité [safetyTimeoutMs] = 1000ms : au cas où ACTION_UP
 *   n'arrive jamais (télécommandes IR sans signal de relâchement fiable)
 *
 * @param thresholdMs     Délai avant déclenchement long press (par défaut 500ms)
 * @param safetyTimeoutMs Timeout de sécurité pour déclencher SHORT si pas d'ACTION_UP
 * @param onShortPress    Callback déclenché sur pression courte
 * @param onLongPress     Callback déclenché sur pression longue
 */
class LongPressDetector(
    private val thresholdMs: Long = DEFAULT_THRESHOLD_MS,
    private val safetyTimeoutMs: Long = SAFETY_TIMEOUT_MS,
    private val onShortPress: () -> Unit,
    private val onLongPress: () -> Unit
) {

    companion object {
        const val DEFAULT_THRESHOLD_MS: Long = 500L
        const val SAFETY_TIMEOUT_MS: Long = 1000L
    }

    private val handler: Handler = Handler(Looper.getMainLooper())

    /** Indique si un ACTION_DOWN est en attente (timer actif). */
    private var isPending: Boolean = false

    /** Consume el ACTION_UP correspondiente después de ejecutar una pulsación larga. */
    private var consumeNextActionUp: Boolean = false

    /** Runnable déclenchant le long press une fois le seuil atteint */
    private val longPressRunnable: Runnable = Runnable {
        if (isPending) {
            isPending = false
            consumeNextActionUp = true
            handler.removeCallbacks(safetyRunnable)
            onLongPress()
        }
    }

    /** Runnable de sécurité — déclenche SHORT si ACTION_UP n'arrive jamais */
    private val safetyRunnable: Runnable = Runnable {
        if (isPending) {
            isPending = false
            consumeNextActionUp = true
            handler.removeCallbacks(longPressRunnable)
            onShortPress()
        }
    }

    /**
     * Traite un KeyEvent et retourne true si l'événement a été consommé.
     *
     * - ACTION_DOWN (repeatCount=0) : démarre les timers
     * - ACTION_DOWN (repeatCount>0) : répétitions ignorées (consommées si timer actif)
     * - ACTION_UP : déclenche short press si timer encore actif
     */
    fun onKeyEvent(event: KeyEvent): Boolean {
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> handleActionDown(event)
            KeyEvent.ACTION_UP   -> handleActionUp()
            else                 -> false
        }
    }

    /** Annule tous les timers en cours sans déclencher de callback. */
    fun cancel() {
        isPending = false
        consumeNextActionUp = false
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(safetyRunnable)
    }

    private fun handleActionDown(event: KeyEvent): Boolean {
        // Ignorer les répétitions auto (touche maintenue)
        if (event.repeatCount > 0) return isPending

        if (!isPending) {
            consumeNextActionUp = false
            isPending = true
            handler.postDelayed(longPressRunnable, thresholdMs)
            handler.postDelayed(safetyRunnable, safetyTimeoutMs)
        }
        return true
    }

    private fun handleActionUp(): Boolean {
        if (!isPending) {
            if (consumeNextActionUp) {
                consumeNextActionUp = false
                return true
            }
            return false
        }

        isPending = false
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(safetyRunnable)
        onShortPress()
        return true
    }
}
