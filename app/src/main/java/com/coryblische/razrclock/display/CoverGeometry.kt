package com.coryblische.razrclock.display

import android.view.DisplayCutout
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.max
import kotlin.math.min

/**
 * Razr Ultra 2025 cover (leap / XT2551), display 1, 1080x1272.
 * Camera cutout bounding rect (585,923)-(1080,1272). Fractions keep the same
 * corner clear if density or a screenshot device rounds the pixel size.
 */
object CoverGeometry {
    const val LEFT_FRACTION = 585f / 1080f
    const val TOP_FRACTION = 923f / 1272f
    private const val ASPECT_MIN = 0.80f
    private const val ASPECT_MAX = 0.93f

    fun isCoverAspect(width: Float, height: Float): Boolean {
        if (width <= 0f || height <= 0f) return false
        val aspect = width / height
        return aspect in ASPECT_MIN..ASPECT_MAX
    }

    fun avoidRect(width: Float, height: Float, cutout: DisplayCutout?): Rect? {
        val rects = ArrayList<Rect>()
        if (isCoverAspect(width, height)) {
            rects += Rect(
                width * LEFT_FRACTION,
                height * TOP_FRACTION,
                width,
                height,
            )
        }
        cutout?.boundingRects?.forEach { raw ->
            if (raw.top > height * 0.55f && raw.width() > 0 && raw.height() > 0) {
                rects += Rect(
                    raw.left.toFloat(),
                    raw.top.toFloat(),
                    raw.right.toFloat(),
                    raw.bottom.toFloat(),
                )
            }
        }
        if (rects.isEmpty()) return null
        return Rect(
            rects.minOf { it.left },
            rects.minOf { it.top },
            rects.maxOf { it.right },
            rects.maxOf { it.bottom },
        )
    }
}

data class ClockRegions(
    val frame: Path,
    val prompt: Rect,
    val upper: Rect,
    val lower: Rect,
)

fun clockRegions(
    width: Float,
    height: Float,
    margin: Float,
    promptHeight: Float,
    avoid: Rect?,
): ClockRegions {
    val left = margin
    val top = margin
    val right = width - margin
    val bottom = height - margin
    val path = Path()
    val pad = 12f
    val cut = avoid?.takeIf { it.left > left + 64f && it.top < bottom - 48f && it.top > top + promptHeight + 80f }
    if (cut == null) {
        path.addRect(Rect(left, top, right, bottom))
        val prompt = Rect(left + pad, top + pad, right - pad, top + pad + promptHeight)
        val lowerTop = max(prompt.bottom + 24f, bottom - height * 0.18f)
        return ClockRegions(
            frame = path,
            prompt = prompt,
            upper = Rect(left + pad, prompt.bottom + 8f, right - pad, lowerTop - 8f),
            lower = Rect(left + pad, lowerTop, right - pad, bottom - pad),
        )
    }
    val cutX = min(right - 8f, max(left + 120f, cut.left - 8f))
    val cutY = min(bottom - 48f, max(top + promptHeight + 80f, cut.top - 8f))
    path.moveTo(left, top)
    path.lineTo(right, top)
    path.lineTo(right, cutY)
    path.lineTo(cutX, cutY)
    path.lineTo(cutX, bottom)
    path.lineTo(left, bottom)
    path.close()
    val prompt = Rect(left + pad, top + pad, right - pad, top + pad + promptHeight)
    return ClockRegions(
        frame = path,
        prompt = prompt,
        upper = Rect(left + pad, prompt.bottom + 10f, right - pad, cutY - 12f),
        lower = Rect(left + pad, cutY + 12f, cutX - 12f, bottom - pad),
    )
}
