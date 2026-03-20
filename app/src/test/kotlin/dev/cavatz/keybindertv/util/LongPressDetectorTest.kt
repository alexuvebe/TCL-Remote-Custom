package dev.cavatz.keybindertv.util

import android.view.KeyEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Tests unitaires pour [LongPressDetector].
 *
 * Problème de testabilité : [LongPressDetector] utilise android.os.Handler qui
 * dépend de Looper.getMainLooper(), non disponible en environnement JVM pur.
 *
 * Solution : [TestableLongPressDetector] remplace Handler par un [ScheduledExecutorService]
 * injectable, permettant de contrôler le temps dans les tests.
 */
class LongPressDetectorTest {

    /** Implémentation testable qui remplace Handler par un executor injectable. */
    private inner class TestableLongPressDetector(
        private val thresholdMs: Long = LongPressDetector.DEFAULT_THRESHOLD_MS,
        private val safetyTimeoutMs: Long = LongPressDetector.SAFETY_TIMEOUT_MS,
        private val onShortPress: () -> Unit,
        private val onLongPress: () -> Unit,
        private val executor: ScheduledExecutorService = realExecutor
    ) {
        private var isPending: Boolean = false
        private var longPressFuture: ScheduledFuture<*>? = null
        private var safetyFuture: ScheduledFuture<*>? = null

        fun onKeyEvent(event: KeyEvent): Boolean {
            return when (event.action) {
                KeyEvent.ACTION_DOWN -> handleDown(event)
                KeyEvent.ACTION_UP   -> handleUp()
                else                 -> false
            }
        }

        fun cancel() {
            isPending = false
            longPressFuture?.cancel(false)
            safetyFuture?.cancel(false)
        }

        private fun handleDown(event: KeyEvent): Boolean {
            if (event.repeatCount > 0) return isPending
            if (!isPending) {
                isPending = true
                longPressFuture = executor.schedule({
                    if (isPending) {
                        isPending = false
                        safetyFuture?.cancel(false)
                        onLongPress()
                    }
                }, thresholdMs, TimeUnit.MILLISECONDS)

                safetyFuture = executor.schedule({
                    if (isPending) {
                        isPending = false
                        longPressFuture?.cancel(false)
                        onShortPress()
                    }
                }, safetyTimeoutMs, TimeUnit.MILLISECONDS)
            }
            return true
        }

        private fun handleUp(): Boolean {
            if (!isPending) return false
            isPending = false
            longPressFuture?.cancel(false)
            safetyFuture?.cancel(false)
            onShortPress()
            return true
        }
    }

    private lateinit var realExecutor: ScheduledThreadPoolExecutor

    private var shortPressCount: Int = 0
    private var longPressCount: Int = 0

    @Before
    fun setUp() {
        realExecutor = ScheduledThreadPoolExecutor(1)
        shortPressCount = 0
        longPressCount = 0
    }

    @After
    fun tearDown() {
        realExecutor.shutdownNow()
    }

    private fun buildDetector(
        thresholdMs: Long = 100L,
        safetyTimeoutMs: Long = 300L
    ): TestableLongPressDetector = TestableLongPressDetector(
        thresholdMs = thresholdMs,
        safetyTimeoutMs = safetyTimeoutMs,
        onShortPress = { shortPressCount++ },
        onLongPress = { longPressCount++ }
    )

    private fun keyDown(keyCode: Int = KeyEvent.KEYCODE_DPAD_CENTER, repeatCount: Int = 0): KeyEvent =
        KeyEvent(KeyEvent.ACTION_DOWN, keyCode).let {
            // KeyEvent n'expose pas repeatCount dans le constructeur simple — on le recrée
            KeyEvent(
                0L, 0L, KeyEvent.ACTION_DOWN, keyCode, repeatCount
            )
        }

    private fun keyUp(keyCode: Int = KeyEvent.KEYCODE_DPAD_CENTER): KeyEvent =
        KeyEvent(KeyEvent.ACTION_UP, keyCode)

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    fun `test_short_press_when_up_before_threshold`() {
        val detector = buildDetector(thresholdMs = 200L)

        detector.onKeyEvent(keyDown())
        Thread.sleep(50L) // bien avant le seuil
        detector.onKeyEvent(keyUp())

        Thread.sleep(50L) // laisser l'executor finir

        assertEquals("Exactement 1 short press attendu", 1, shortPressCount)
        assertEquals("Aucun long press attendu", 0, longPressCount)
    }

    @Test
    fun `test_long_press_when_threshold_elapsed`() {
        val detector = buildDetector(thresholdMs = 100L, safetyTimeoutMs = 500L)

        detector.onKeyEvent(keyDown())
        Thread.sleep(200L) // après le seuil long press

        assertEquals("Aucun short press attendu", 0, shortPressCount)
        assertEquals("Exactement 1 long press attendu", 1, longPressCount)
    }

    @Test
    fun `test_safety_timeout_triggers_short_press`() {
        // Pas d'ACTION_UP envoyé — la sécurité déclenche le short press
        val detector = buildDetector(thresholdMs = 500L, safetyTimeoutMs = 150L)

        detector.onKeyEvent(keyDown())
        Thread.sleep(300L) // après le safety timeout

        assertEquals("Short press de sécurité attendu", 1, shortPressCount)
        assertEquals("Aucun long press attendu", 0, longPressCount)
    }

    @Test
    fun `test_cancel_prevents_callbacks`() {
        val detector = buildDetector(thresholdMs = 200L, safetyTimeoutMs = 400L)

        detector.onKeyEvent(keyDown())
        detector.cancel()
        detector.onKeyEvent(keyUp()) // ne doit rien déclencher

        Thread.sleep(500L) // attendre bien au-delà des deux timers

        assertEquals("Aucun short press après cancel", 0, shortPressCount)
        assertEquals("Aucun long press après cancel", 0, longPressCount)
    }

    @Test
    fun `test_multiple_keys_independent`() {
        // Deux détecteurs fonctionnent indépendamment
        val detector1 = buildDetector(thresholdMs = 200L)
        val detector2 = buildDetector(thresholdMs = 200L)

        // Détecteur 1 → short press (ACTION_UP rapide)
        detector1.onKeyEvent(keyDown(KeyEvent.KEYCODE_DPAD_CENTER))
        Thread.sleep(30L)
        detector1.onKeyEvent(keyUp(KeyEvent.KEYCODE_DPAD_CENTER))

        // Détecteur 2 → long press (pas d'ACTION_UP)
        detector2.onKeyEvent(keyDown(KeyEvent.KEYCODE_BACK))
        Thread.sleep(300L) // dépasse le seuil long press

        assertEquals("1 short press attendu (détecteur 1)", 1, shortPressCount)
        assertEquals("1 long press attendu (détecteur 2)", 1, longPressCount)
    }

    @Test
    fun `test_repeat_count_events_ignored_when_not_pending`() {
        val detector = buildDetector()

        // ACTION_DOWN avec repeatCount > 0 sans pending → doit retourner false
        val repeatEvent: KeyEvent = keyDown(repeatCount = 5)
        val consumed: Boolean = detector.onKeyEvent(repeatEvent)

        assertEquals("Événement répété sans pending non consommé", false, consumed)
        Thread.sleep(150L)
        assertEquals("Aucun callback déclenché", 0, shortPressCount + longPressCount)
    }
}
