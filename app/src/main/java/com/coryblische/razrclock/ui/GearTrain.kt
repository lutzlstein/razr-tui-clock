package com.coryblische.razrclock.ui

import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Compound idler between the two lens rims.
 *
 * The bezels are 227 px apart and only about 4 px clear of each other, so the
 * rims cannot mesh on the tangent (that point is under the glass). A two-step
 * idler sits in the open notch above the tangent.
 *
 * Lens L (60 teeth) advances one tooth per wall-clock second. The idler's
 * 40-tooth wheel takes that tooth and its 8-tooth pinion, fixed to the same
 * shaft, drives lens R. R therefore advances 8/40 of a tooth per second, which
 * is one full tooth every 5 seconds. A 15:1 pair does not fit in the notch.
 * Four marker teeth on L, 15 teeth apart, meet a fixed index every 15 seconds.
 *
 * External meshes turn opposite ways. Both stages share module 4, so the
 * circular pitch matches and a tooth never lands on a tooth.
 */
object GearTrain {
    const val MODULE = 4f
    const val TEETH_L = 60
    const val TEETH_R = 62
    const val TEETH_WHEEL = 40
    const val TEETH_PINION = 8

    /** Seconds for lens R to advance exactly one tooth. */
    const val RIGHT_TOOTH_SECONDS = TEETH_WHEEL / TEETH_PINION

    /** Marker teeth on L are this many teeth apart, so the index is met every 15 s. */
    const val MARKER_EVERY = 15

    fun pitchRadius(teeth: Int, module: Float = MODULE): Float = teeth * module / 2f

    fun pose(epochSecond: Long, leftX: Float, leftY: Float, rightX: Float, rightY: Float, module: Float = MODULE): GearPose {
        val radiusL = pitchRadius(TEETH_L, module)
        val radiusWheel = pitchRadius(TEETH_WHEEL, module)
        val radiusPinion = pitchRadius(TEETH_PINION, module)
        val radiusR = pitchRadius(TEETH_R, module)
        val center = upperNotch(
            leftX, leftY, radiusL + radiusWheel,
            rightX, rightY, radiusR + radiusPinion,
        )
        val towardWheel = degrees(center.y - leftY, center.x - leftX)
        val towardRight = degrees(rightY - center.y, rightX - center.x)
        val towardIdle = towardRight + 180f
        val toothL = 360f / TEETH_L
        val toothWheel = 360f / TEETH_WHEEL
        val toothR = 360f / TEETH_R
        // Modulo in integer seconds first. A raw epoch does not fit in a Float,
        // and dropping low bits would freeze the gears.
        val lapL = (epochSecond % TEETH_L).toFloat()
        val lapWheel = (epochSecond % TEETH_WHEEL).toFloat()
        val lapRight = (epochSecond % (TEETH_R.toLong() * RIGHT_TOOTH_SECONDS)).toFloat()
        // Tooth 0 of L points at the idler. The wheel's gap, not a tooth, faces it.
        val thetaL = towardWheel + lapL * toothL
        val thetaWheel = (towardWheel + 180f) - toothWheel / 2f - lapWheel * toothWheel
        // The pinion is keyed so tooth 0 points at R when the shaft is at its t = 0 angle.
        val assembly = towardRight - ((towardWheel + 180f) - toothWheel / 2f)
        val thetaPinion = thetaWheel + assembly
        // R's gap faces the idler, and it takes TEETH_PINION / TEETH_WHEEL teeth per second.
        val thetaR = towardIdle - toothR / 2f + lapRight * (TEETH_PINION.toFloat() / TEETH_WHEEL) * toothR
        return GearPose(
            idleX = center.x,
            idleY = center.y,
            thetaL = thetaL,
            thetaWheel = thetaWheel,
            thetaPinion = thetaPinion,
            thetaR = thetaR,
            bearingFromLeft = towardWheel,
            bearingFromIdle = towardRight,
            indexAngle = towardWheel + 270f,
        )
    }

    private fun upperNotch(
        leftX: Float,
        leftY: Float,
        reachLeft: Float,
        rightX: Float,
        rightY: Float,
        reachRight: Float,
    ): Point {
        val dx = rightX - leftX
        val dy = rightY - leftY
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        val along = (reachLeft * reachLeft - reachRight * reachRight + distance * distance) / (2f * distance)
        val height = sqrt((reachLeft * reachLeft - along * along).coerceAtLeast(0f))
        val ux = dx / distance
        val uy = dy / distance
        // Perpendicular that points up the screen (decreasing y).
        val px = uy
        val py = -ux
        return Point(
            leftX + along * ux + height * px,
            leftY + along * uy + height * py,
        )
    }

    private fun degrees(dy: Float, dx: Float): Float =
        Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

    private data class Point(val x: Float, val y: Float)
}

data class GearPose(
    val idleX: Float,
    val idleY: Float,
    val thetaL: Float,
    val thetaWheel: Float,
    val thetaPinion: Float,
    val thetaR: Float,
    val bearingFromLeft: Float,
    val bearingFromIdle: Float,
    val indexAngle: Float,
)
