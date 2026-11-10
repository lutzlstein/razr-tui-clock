package com.coryblische.razrclock.ui

/**
 * Both lenses share one angular lattice. A segment is 120°, and each tick
 * is one eighth of that (15°), so a step always lands on the same maze
 * pattern instead of halfway through a gap.
 *
 * Lens 1 advances one step every wall-clock second. Lens 2 advances one
 * step at :00, :15, :30, and :45.
 */
object RingMotion {
    const val SEGMENTS = 3
    const val SEGMENT_PITCH_DEGREES = 360f / SEGMENTS
    const val STEP_DEGREES = SEGMENT_PITCH_DEGREES / 8f
    const val STEPS_PER_TURN = 24L
    const val SLOW_PERIOD_SECONDS = 15L

    fun fastDegrees(epochSecond: Long): Float = (epochSecond % STEPS_PER_TURN) * STEP_DEGREES

    fun slowDegrees(epochSecond: Long): Float =
        ((epochSecond / SLOW_PERIOD_SECONDS) % STEPS_PER_TURN) * STEP_DEGREES

    /** Rings stay staggered by whole steps, so a tick moves them together. */
    fun ringOffset(ring: Int): Float = ring * STEP_DEGREES
}
