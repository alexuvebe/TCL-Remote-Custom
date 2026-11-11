package dev.cavatz.keybindertv.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeDelayedTaskScheduler : DelayedTaskScheduler {
    private data class Entry(val task: Runnable, val dueAt: Long)
    private val entries = mutableListOf<Entry>()
    private var now = 0L

    override fun postDelayed(task: Runnable, delayMillis: Long) {
        removeCallbacks(task)
        entries += Entry(task, now + delayMillis)
    }

    override fun removeCallbacks(task: Runnable) {
        entries.removeAll { it.task === task }
    }

    fun advanceBy(milliseconds: Long) {
        val target = now + milliseconds
        while (true) {
            val next = entries.filter { it.dueAt <= target }.minByOrNull { it.dueAt } ?: break
            entries.remove(next)
            now = next.dueAt
            next.task.run()
        }
        now = target
    }
}

/** Pruebas unitarias del detector de pulsaciones real con un reloj simulado. */
class LongPressDetectorTest {

    private lateinit var scheduler: FakeDelayedTaskScheduler
    private var shortPressCount = 0
    private var longPressCount = 0

    @Before
    fun setUp() {
        scheduler = FakeDelayedTaskScheduler()
        shortPressCount = 0
        longPressCount = 0
    }

    private fun buildDetector(
        thresholdMs: Long = 200L,
        safetyTimeoutMs: Long = thresholdMs + LongPressDetector.SAFETY_TIMEOUT_MS
    ) = LongPressDetector(
        thresholdMs = thresholdMs,
        safetyTimeoutMs = safetyTimeoutMs,
        scheduler = scheduler,
        onShortPress = { shortPressCount++ },
        onLongPress = { longPressCount++ }
    )

    @Test
    fun test_short_press_when_up_before_threshold() {
        val detector = buildDetector(thresholdMs = 200L)
        detector.onKeyDown()
        scheduler.advanceBy(50L)
        detector.onKeyUp()

        assertEquals(1, shortPressCount)
        assertEquals(0, longPressCount)
    }

    @Test
    fun test_long_press_when_threshold_elapsed() {
        val detector = buildDetector(thresholdMs = 100L, safetyTimeoutMs = 500L)
        detector.onKeyDown()
        scheduler.advanceBy(150L)

        assertEquals(0, shortPressCount)
        assertEquals(1, longPressCount)
        detector.onKeyUp()
    }

    @Test
    fun test_long_press_at_maximum_configured_threshold() {
        val detector = buildDetector(thresholdMs = 2000L)
        detector.onKeyDown()
        scheduler.advanceBy(2100L)

        assertEquals(0, shortPressCount)
        assertEquals(1, longPressCount)
        detector.onKeyUp()
    }

    @Test
    fun test_safety_timeout_triggers_short_press() {
        val detector = buildDetector(thresholdMs = 500L, safetyTimeoutMs = 150L)
        detector.onKeyDown()
        scheduler.advanceBy(200L)

        assertEquals(1, shortPressCount)
        assertEquals(0, longPressCount)
    }

    @Test
    fun test_cancel_prevents_callbacks() {
        val detector = buildDetector(thresholdMs = 200L, safetyTimeoutMs = 400L)
        detector.onKeyDown()
        detector.cancel()
        scheduler.advanceBy(500L)

        assertEquals(0, shortPressCount)
        assertEquals(0, longPressCount)
    }

    @Test
    fun test_multiple_keys_independent() {
        val detector1 = buildDetector(thresholdMs = 200L)
        detector1.onKeyDown()
        scheduler.advanceBy(30L)
        detector1.onKeyUp()

        val detector2 = buildDetector(thresholdMs = 200L)
        detector2.onKeyDown()
        scheduler.advanceBy(250L)

        assertEquals(1, shortPressCount)
        assertEquals(1, longPressCount)
        detector1.cancel()
        detector2.cancel()
    }

    @Test
    fun test_repeat_count_events_ignored_when_not_pending() {
        val detector = buildDetector()
        assertEquals(false, detector.onKeyDown(repeatCount = 5))
        scheduler.advanceBy(150L)
        assertEquals(0, shortPressCount + longPressCount)
        detector.cancel()
    }
}
