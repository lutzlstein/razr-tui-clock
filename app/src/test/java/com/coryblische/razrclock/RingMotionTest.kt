package com.coryblische.razrclock

import com.coryblische.razrclock.ui.RingMotion
import org.junit.Assert.assertEquals
import org.junit.Test

class RingMotionTest {
    @Test
    fun stepDividesTheSegmentPitch() {
        val stepsPerSegment = RingMotion.SEGMENT_PITCH_DEGREES / RingMotion.STEP_DEGREES
        assertEquals(8f, stepsPerSegment, 0.001f)
        assertEquals(0f, RingMotion.SEGMENT_PITCH_DEGREES % RingMotion.STEP_DEGREES, 0.001f)
    }

    @Test
    fun fastRingAdvancesOncePerSecondAndSlowRingOnTheQuarterMinute() {
        val t = 1_700_000_010L
        assertEquals(RingMotion.STEP_DEGREES, RingMotion.fastDegrees(t + 1) - RingMotion.fastDegrees(t), 0.001f)
        assertEquals(RingMotion.slowDegrees(t), RingMotion.slowDegrees(t + 14), 0.001f)
        assertEquals(
            RingMotion.STEP_DEGREES,
            RingMotion.slowDegrees(t + 15) - RingMotion.slowDegrees(t),
            0.001f,
        )
    }

    @Test
    fun quarterMinuteBoundariesAreTheSlowTicks() {
        val base = 1_700_000_040L // divisible by 15
        assertEquals(0L, base % 15L)
        assertEquals(RingMotion.slowDegrees(base), RingMotion.slowDegrees(base + 14), 0.001f)
        assertEquals(RingMotion.slowDegrees(base - 1), RingMotion.slowDegrees(base) - RingMotion.STEP_DEGREES, 0.001f)
    }
}
