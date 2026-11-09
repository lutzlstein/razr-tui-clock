package com.coryblische.razrclock.data

/**
 * Lens centers and glass radii in the cover's 1080×1272 pixel space.
 * Defaults are [CoverHoles], scaled from Motorola's external_display_holes.png.
 * The radii are the glass/bezel, not the maze. Rings are drawn outside them.
 */
data class LensPair(
    val ax: Float = CoverHoles.lensL.x,
    val ay: Float = CoverHoles.lensL.y,
    val ar: Float = CoverHoles.lensL.radius,
    val bx: Float = CoverHoles.lensR.x,
    val by: Float = CoverHoles.lensR.y,
    val br: Float = CoverHoles.lensR.radius,
) {
    fun x(index: Int): Float = if (index == 0) ax else bx
    fun y(index: Int): Float = if (index == 0) ay else by
    fun r(index: Int): Float = if (index == 0) ar else br

    fun moved(index: Int, dx: Float, dy: Float): LensPair {
        val nx = (x(index) + dx).coerceIn(0f, REFERENCE_WIDTH)
        val ny = (y(index) + dy).coerceIn(0f, REFERENCE_HEIGHT)
        return placed(index, nx, ny, r(index))
    }

    fun scaledRadius(index: Int, factor: Float): LensPair {
        val nr = (r(index) * factor).coerceIn(MIN_RADIUS, MAX_RADIUS)
        return placed(index, x(index), y(index), nr)
    }

    fun radiusDelta(index: Int, delta: Float): LensPair {
        val nr = (r(index) + delta).coerceIn(MIN_RADIUS, MAX_RADIUS)
        return placed(index, x(index), y(index), nr)
    }

    fun centersInsideCutout(): Boolean {
        fun inside(x: Float, y: Float) = x in CUTOUT_LEFT..CUTOUT_RIGHT && y in CUTOUT_TOP..CUTOUT_BOTTOM
        return inside(ax, ay) && inside(bx, by)
    }

    private fun placed(index: Int, x: Float, y: Float, r: Float): LensPair = if (index == 0) {
        copy(ax = x, ay = y, ar = r)
    } else {
        copy(bx = x, by = y, br = r)
    }

    companion object {
        const val REFERENCE_WIDTH = 1080f
        const val REFERENCE_HEIGHT = 1272f
        const val MIN_RADIUS = 16f
        const val MAX_RADIUS = 220f
        const val CUTOUT_LEFT = 585f
        const val CUTOUT_TOP = 923f
        const val CUTOUT_RIGHT = 1080f
        const val CUTOUT_BOTTOM = 1272f
    }
}
