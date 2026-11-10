package com.coryblische.razrclock

import com.coryblische.razrclock.ui.GearTrain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class GearTrainTest {
    private fun pose(second: Long) = GearTrain.pose(second, 721f, 1090f, 948f, 1092f)

    @Test
    fun lensCentersLeaveOnlyAFewPixelsBetweenTheBezels() {
        val gap = hypot(948.0 - 721.0, 1092.0 - 1090.0) - 110.0 - 113.0
        assertEquals(4.0, gap, 0.05)
    }

    @Test
    fun leftGearAdvancesOneToothPerSecondAndTheRightGearOneToothEveryFive() {
        val toothL = 360f / GearTrain.TEETH_L
        val toothR = 360f / GearTrain.TEETH_R
        val a = pose(1_700_000_000L)
        val b = pose(1_700_000_001L)
        assertEquals(toothL, signed(b.thetaL - a.thetaL), 0.02f)
        assertEquals(-360f / GearTrain.TEETH_WHEEL, signed(b.thetaWheel - a.thetaWheel), 0.02f)
        assertEquals(toothR / GearTrain.RIGHT_TOOTH_SECONDS, signed(b.thetaR - a.thetaR), 0.02f)
        val five = pose(1_700_000_005L)
        assertEquals(toothR, wrap(five.thetaR - a.thetaR), 0.02f)
    }

    @Test
    fun meshedTeethStayInTheGapsAtEverySecond() {
        val bearing = pose(0L).bearingFromLeft
        repeat(30) { second ->
            val pose = pose(second.toLong())
            assertEquals(0f, nearestTooth(pose.thetaL, GearTrain.TEETH_L, bearing), 0.05f)
            val gapFacingLeft = pose.thetaWheel + 180f / GearTrain.TEETH_WHEEL
            assertEquals(0f, nearestTooth(gapFacingLeft, GearTrain.TEETH_WHEEL, bearing + 180f), 0.05f)
            // R and the pinion stay a matching fraction of a tooth apart, so a tooth
            // sits in a gap even on the seconds between whole-tooth steps.
            val pinionPitch = 360f / GearTrain.TEETH_PINION
            val rightPitch = 360f / GearTrain.TEETH_R
            val pinionFraction = wrap(pose.bearingFromIdle - pose.thetaPinion) / pinionPitch
            val rightGapFraction = wrap(pose.bearingFromIdle + 180f - (pose.thetaR + rightPitch / 2f)) / rightPitch
            // Opposite rotation: the two fractions sum to zero when a tooth stays in a gap.
            assertEquals(0f, fractional(pinionFraction) + fractional(rightGapFraction), 0.02f)
            if (second % GearTrain.RIGHT_TOOTH_SECONDS == 0) {
                assertEquals(0f, nearestTooth(pose.thetaPinion, GearTrain.TEETH_PINION, pose.bearingFromIdle), 0.05f)
            }
        }
    }

    @Test
    fun idlerSitsAboveTheTangentAndOffBothBezels() {
        val pose = pose(0L)
        val toLeft = hypot((pose.idleX - 721f).toDouble(), (pose.idleY - 1090f).toDouble())
        val toRight = hypot((pose.idleX - 948f).toDouble(), (pose.idleY - 1092f).toDouble())
        assertTrue(pose.idleY < 1091f)
        assertTrue(toLeft > 110.0 + GearTrain.pitchRadius(GearTrain.TEETH_WHEEL))
        assertTrue(toRight > 113.0 + GearTrain.pitchRadius(GearTrain.TEETH_PINION))
    }

    private fun nearestTooth(theta: Float, teeth: Int, target: Float): Float {
        val pitch = 360f / teeth
        val relative = wrap(target - theta)
        val into = relative % pitch
        return minOf(into, pitch - into)
    }

    private fun fractional(value: Float): Float {
        var wrapped = value % 1f
        if (wrapped < 0f) wrapped += 1f
        return if (wrapped > 0.5f) wrapped - 1f else wrapped
    }

    private fun signed(degrees: Float): Float {
        var value = wrap(degrees)
        if (value > 180f) value -= 360f
        return value
    }

    private fun wrap(degrees: Float): Float {
        var value = degrees % 360f
        if (value < 0f) value += 360f
        return value
    }

}
