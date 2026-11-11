package dev.cavatz.keybindertv.util

import android.os.Looper
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import java.util.concurrent.TimeUnit

/**
 * Pruebas del detector real usando el Looper principal simulado por Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class LongPressDetectorTest {

    private var shortPressCount = 0
    private var longPressCount = 0

    @Before
    fun setUp() {
        shortPressCount = 0
        longPressCount = 0
    }

    private fun buildDetector(
        thresholdMs: Long = 200L,
        safetyTimeoutMs: Long = thresholdMs + LongPressDetector.SAFETY_TIMEOUT_MS
    ) = LongPressDetector(
        thresholdMs = thresholdMs,
        safetyTimeoutMs = safetyTimeoutMs,
        onShortPress = { shortPressCount++ },
        onLongPress = { longPressCount++ }
    )

    private fun keyDown(
        keyCode: Int = KeyEvent.KEYCODE_DPAD_CENTER,
        repeatCount: Int = 0
    ) = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, repeatCount)

    private fun keyUp(keyCode: Int = KeyEvent.KEYCODE_DPAD_CENTER) =
        KeyEvent(KeyEvent.ACTION_UP, keyCode)

    private fun advanceTime(milliseconds: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(milliseconds, TimeUnit.MILLISECONDS)
    }

    @Test
    fun test_short_press_when_up_before_threshold() {
        val detector = buildDetector(thresholdMs = 200L)
        detector.onKeyEvent(keyDown())
        advanceTime(50L)
        detector.onKeyEvent(keyUp())

        assertEquals(1, shortPressCount)
        assertEquals(0, longPressCount)
    }

    @Test
    fun test_long_press_when_threshold_elapsed() {
        val detector = buildDetector(thresholdMs = 100L, safetyTimeoutMs = 500L)
        detector.onKeyEvent(keyDown())
        advanceTime(150L)

        assertEquals(0, shortPressCount)
        assertEquals(1, longPressCount)
        detector.onKeyEvent(keyUp())
    }

    @Test
    fun test_long_press_at_maximum_configured_threshold() {
        val detector = buildDetector(thresholdMs = 2000L)
        detector.onKeyEvent(keyDown())
        advanceTime(2100L)

        assertEquals(0, shortPressCount)
        assertEquals(1, longPressCount)
        detector.onKeyEvent(keyUp())
    }

    @Test
    fun test_safety_timeout_triggers_short_press() {
        val detector = buildDetector(thresholdMs = 500L, safetyTimeoutMs = 150L)
        detector.onKeyEvent(keyDown())
        advanceTime(200L)

        assertEquals(1, shortPressCount)
        assertEquals(0, longPressCount)
    }

    @Test
    fun test_cancel_prevents_callbacks() {
        val detector = buildDetector(thresholdMs = 200L, safetyTimeoutMs = 400L)
        detector.onKeyEvent(keyDown())
        detector.cancel()
        advanceTime(500L)

        assertEquals(0, shortPressCount)
        assertEquals(0, longPressCount)
    }

    @Test
    fun test_multiple_keys_independent() {
        val detector1 = buildDetector(thresholdMs = 200L)
        val detector2 = buildDetector(thresholdMs = 200L)

        detector1.onKeyEvent(keyDown(KeyEvent.KEYCODE_DPAD_CENTER))
        advanceTime(30L)
        detector1.onKeyEvent(keyUp(KeyEvent.KEYCODE_DPAD_CENTER))

        detector2.onKeyEvent(keyDown(KeyEvent.KEYCODE_BACK))
        advanceTime(250L)

        assertEquals(1, shortPressCount)
        assertEquals(1, longPressCount)
        detector1.cancel()
        detector2.cancel()
    }

    @Test
    fun test_repeat_count_events_ignored_when_not_pending() {
        val detector = buildDetector()
        val repeatEvent = keyDown(repeatCount = 5)

        assertEquals(false, detector.onKeyEvent(repeatEvent))
        advanceTime(150L)
        assertEquals(0, shortPressCount + longPressCount)
        detector.cancel()
    }
}
