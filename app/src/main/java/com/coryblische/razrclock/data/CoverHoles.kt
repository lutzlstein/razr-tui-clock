package com.coryblische.razrclock.data

/**
 * Motorola CliSettings `res/drawable-nodpi-v4/external_display_holes.png`
 * is a 148×75 glyph. Scale it to the cover cutout width (495 / 148) and
 * center that glyph vertically in the cutout (585,923)-(1080,1272).
 *
 * Published cover-pixel results, rounded to 0.1:
 * lens L (777.3, 1094.2) r88.6
 * lens R (962.7, 1094.1) r88.7
 * flash  (631.3, 1095.3) r28.3
 */
object CoverHoles {
    const val GLYPH_WIDTH = 148f
    const val GLYPH_HEIGHT = 75f
    const val CUTOUT_LEFT = 585f
    const val CUTOUT_TOP = 923f
    const val CUTOUT_WIDTH = 495f
    const val CUTOUT_HEIGHT = 349f
    const val SCALE = CUTOUT_WIDTH / GLYPH_WIDTH

    const val LENS_L_GLYPH_X = 57.50f
    const val LENS_L_GLYPH_Y = 36.50f
    const val LENS_L_GLYPH_R = 26.50f
    const val LENS_R_GLYPH_X = 112.92f
    const val LENS_R_GLYPH_Y = 36.48f
    const val LENS_R_GLYPH_R = 26.53f
    const val FLASH_GLYPH_X = 13.85f
    const val FLASH_GLYPH_Y = 36.85f
    const val FLASH_GLYPH_R = 8.46f

    val glyphTop: Float =
        CUTOUT_TOP + (CUTOUT_HEIGHT - GLYPH_HEIGHT * SCALE) / 2f

    fun x(glyphX: Float): Float = CUTOUT_LEFT + glyphX * SCALE
    fun y(glyphY: Float): Float = glyphTop + glyphY * SCALE
    fun r(glyphR: Float): Float = glyphR * SCALE

    val lensL = Hole(x(LENS_L_GLYPH_X), y(LENS_L_GLYPH_Y), r(LENS_L_GLYPH_R))
    val lensR = Hole(x(LENS_R_GLYPH_X), y(LENS_R_GLYPH_Y), r(LENS_R_GLYPH_R))
    val flash = Hole(x(FLASH_GLYPH_X), y(FLASH_GLYPH_Y), r(FLASH_GLYPH_R))

    data class Hole(val x: Float, val y: Float, val radius: Float)
}
