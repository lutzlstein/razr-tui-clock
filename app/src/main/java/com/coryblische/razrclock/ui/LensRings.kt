package com.coryblische.razrclock.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.coryblische.razrclock.data.LensPair
import com.coryblische.razrclock.theme.OmarchyPalette
import kotlin.math.cos
import kotlin.math.sin

data class PlacedLens(
    val center: Offset,
    val radius: Float,
)

fun LensPair.place(index: Int, widthPx: Float, heightPx: Float): PlacedLens {
    val sx = widthPx / LensPair.REFERENCE_WIDTH
    val sy = heightPx / LensPair.REFERENCE_HEIGHT
    val scale = (sx + sy) / 2f
    return PlacedLens(
        center = Offset(x(index) * sx, y(index) * sy),
        radius = r(index) * scale,
    )
}

/**
 * Maze rings sit a few pixels outside the bezel and rotate as a rigid set.
 * The lenses nearly touch, so each set is masked to its own side of the
 * fixed midline. The mask is in screen space and does not depend on the
 * angle, so arcs slide under it instead of being re-cut every frame.
 */
fun DrawScope.drawMazeRings(
    lens: PlacedLens,
    other: PlacedLens,
    rotationDegrees: Float,
    colors: List<Color>,
    emphasized: Boolean,
) {
    val stroke = if (emphasized) 5f else RING_STROKE
    val keepLeft = lens.center.x <= other.center.x
    val midX = (lens.center.x + other.center.x) / 2f
    val maskLeft = if (keepLeft) 0f else midX + stroke
    val maskRight = if (keepLeft) (midX - stroke).coerceAtLeast(maskLeft) else size.width
    clipRect(0f, 0f, size.width, size.height) {
        clipRect(maskLeft, 0f, maskRight, size.height) {
            repeat(RING_COUNT) { ring ->
                val radius = lens.radius + stroke / 2f + RING_CLEARANCE + ring * RING_PITCH
                val color = colors[ring % colors.size]
                val base = rotationDegrees + RingMotion.ringOffset(ring)
                val sweep = RingMotion.SEGMENT_PITCH_DEGREES - GAP_DEGREES
                repeat(RingMotion.SEGMENTS) { segment ->
                    val start = normalize(base + segment * RingMotion.SEGMENT_PITCH_DEGREES)
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(lens.center.x - radius, lens.center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                    drawEndTick(lens, radius, start + sweep, color, stroke, keepLeft, maskLeft, maskRight, ring + segment)
                }
            }
        }
    }
}

/**
 * Ticks belong to the segment end. They are omitted when that end is across
 * the midline, so the mask never grows a tick of its own (the crossed V).
 */
private fun DrawScope.drawEndTick(
    lens: PlacedLens,
    radius: Float,
    endDegrees: Float,
    color: Color,
    stroke: Float,
    keepLeft: Boolean,
    maskLeft: Float,
    maskRight: Float,
    seed: Int,
) {
    val endRad = Math.toRadians(endDegrees.toDouble())
    val turn = if (seed % 2 == 0) 12f else -12f
    val x1 = lens.center.x + cos(endRad).toFloat() * radius
    val y1 = lens.center.y + sin(endRad).toFloat() * radius
    val x2 = lens.center.x + cos(endRad).toFloat() * (radius + turn)
    val y2 = lens.center.y + sin(endRad).toFloat() * (radius + turn)
    val kept = if (keepLeft) x1 <= maskRight && x2 <= maskRight else x1 >= maskLeft && x2 >= maskLeft
    if (kept) drawLine(color, Offset(x1, y1), Offset(x2, y2), stroke, StrokeCap.Square)
}

private fun normalize(degrees: Float): Float {
    var value = degrees % 360f
    if (value < 0f) value += 360f
    return value
}

fun ringColors(palette: OmarchyPalette): List<Color> = listOf(
    palette.accent,
    palette.orange,
    palette.brown,
    palette.muted,
)

private const val RING_COUNT = 3
private const val RING_STROKE = 4f
private const val RING_CLEARANCE = 5f
private const val RING_PITCH = 14f
private const val GAP_DEGREES = 34f
