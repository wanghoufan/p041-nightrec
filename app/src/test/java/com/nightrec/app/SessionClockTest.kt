package com.nightrec.app

import org.junit.Assert.assertEquals
import org.junit.Test

/** T014: monotonic logical time excludes gaps; wall time stays anchored to real clock. */
class SessionClockTest {
    @Test
    fun logicalAdvancesWhileActiveAndWallTracksAnchor() {
        val clock = SessionClock(monotonic = 1000, wall = 10_000)
        assertEquals(1000L, clock.logicalAt(2000))
        assertEquals(11_000L, clock.wallAt(2000))
    }

    @Test
    fun pausedGapDoesNotEnterLogicalTime() {
        val clock = SessionClock(monotonic = 1000, wall = 10_000)
        clock.pause(3000) // accrues 2000ms
        assertEquals(2000L, clock.logicalAt(4000))
        clock.resume(now = 5000, wall = 99_000) // 2s gap not counted
        assertEquals(4000L, clock.logicalAt(7000))
        assertEquals(101_000L, clock.wallAt(7000))
    }

    @Test
    fun negativeMonotonicDeltasAreClampedToZero() {
        val clock = SessionClock(monotonic = 5000, wall = 10_000)
        assertEquals(0L, clock.logicalAt(3000))
        assertEquals(10_000L, clock.wallAt(3000))
    }
}