package com.coryblische.razrclock.data

/**
 * Cover-pixel lens geometry, measured from a photo of the 1.2.1-align grid.
 *
 * A homography from the labeled 50 px grid intersections maps photo pixels
 * into cover pixels (0.38 px RMS over the grid samples). Circles are then
 * fit to the outer edge of each metal bezel and to the flash. Residuals
 * are about 1.3 px (left), 1.0 px (right), and 0.1 px (flash).
 *
 * Reported to 1 px:
 * lens L (721, 1090) r 110
 * lens R (948, 1092) r 113
 * flash  (540, 1088) r 34
 *
 * The 1.2.0 hole-map reading of external_display_holes.png
 * (L 777.3,1094.2 r88.6 / R 962.7,1094.1 r88.7 / flash 631.3,1095.3 r28.3)
 * sits right of the glass and inside the bezel, so it is not used.
 */
object CoverHoles {
    const val CUTOUT_LEFT = 585f
    const val CUTOUT_TOP = 923f
    const val CUTOUT_WIDTH = 495f
    const val CUTOUT_HEIGHT = 349f

    val lensL = Hole(721f, 1090f, 110f)
    val lensR = Hole(948f, 1092f, 113f)
    val flash = Hole(540f, 1088f, 34f)

    data class Hole(val x: Float, val y: Float, val radius: Float)
}
