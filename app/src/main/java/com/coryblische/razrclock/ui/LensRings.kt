package com.coryblische.razrclock.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.coryblische.razrclock.data.LensPair
import com.coryblische.razrclock.theme.OmarchyPalette
import kotlin.math.acos
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
 * Maze rings sit outside the glass. The two lenses nearly touch near x 870,
 * so each ring is arc-clipped at the midline instead of being shrunk onto
 * the glass. Drawing is also clipped to the canvas.
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
    val limitX = if (keepLeft) midX - stroke else midX + stroke
    clipRect(0f, 0f, size.width, size.height) {
        repeat(RING_COUNT) { ring ->
            val radius = lens.radius + stroke / 2f + RING_CLEARANCE + ring * RING_PITCH
            val color = colors[ring % colors.size]
            val allowed = allowedSpans(lens.center.x, radius, limitX, keepLeft)
            val base = rotationDegrees + ring * 19f
            val sweep = (360f / SEGMENTS) - GAP_DEGREES
            repeat(SEGMENTS) { segment ->
                val start = normalize(base + segment * (360f / SEGMENTS))
                val piece = start to start + sweep
                for ((clipStart, clipEnd) in intersectSweep(piece, allowed)) {
                    drawArc(
                        color = color,
                        startAngle = clipStart,
                        sweepAngle = clipEnd - clipStart,
                        useCenter = false,
                        topLeft = Offset(lens.center.x - radius, lens.center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                    val endRad = Math.toRadians(clipEnd.toDouble())
                    val turn = if ((ring + segment) % 2 == 0) 12f else -12f
                    val x1 = lens.center.x + cos(endRad).toFloat() * radius
                    val y1 = lens.center.y + sin(endRad).toFloat() * radius
                    val x2 = lens.center.x + cos(endRad).toFloat() * (radius + turn)
                    val y2 = lens.center.y + sin(endRad).toFloat() * (radius + turn)
                    if (x2 in 0f..size.width && y2 in 0f..size.height) {
                        drawLine(color, Offset(x1, y1), Offset(x2, y2), stroke, StrokeCap.Square)
                    }
                }
            }
        }
    }
}

private fun allowedSpans(cx: Float, radius: Float, limitX: Float, keepLeft: Boolean): List<Pair<Float, Float>> {
    if (radius <= 0f) return emptyList()
    val whollyInside = if (keepLeft) cx + radius <= limitX else cx - radius >= limitX
    if (whollyInside) return listOf(0f to 360f)
    val whollyOutside = if (keepLeft) cx - radius > limitX else cx + radius < limitX
    if (whollyOutside) return emptyList()
    val k = ((limitX - cx) / radius).coerceIn(-1f, 1f)
    val edge = Math.toDegrees(acos(k.toDouble())).toFloat()
    return if (keepLeft) {
        listOf(edge to (360f - edge))
    } else {
        listOf(0f to edge, (360f - edge) to 360f)
    }
}

private fun intersectSweep(
    sweep: Pair<Float, Float>,
    allowed: List<Pair<Float, Float>>,
): List<Pair<Float, Float>> {
    val pieces = unwrap(sweep.first, sweep.second)
    val out = ArrayList<Pair<Float, Float>>()
    for (piece in pieces) {
        for (span in allowed) {
            val start = maxOf(piece.first, span.first)
            val end = minOf(piece.second, span.second)
            if (end - start > 1.5f) out += start to end
        }
    }
    return out
}

private fun unwrap(start: Float, end: Float): List<Pair<Float, Float>> {
    val a = normalize(start)
    val b = a + (end - start)
    return if (b <= 360f) {
        listOf(a to b)
    } else {
        listOf(a to 360f, 0f to (b - 360f))
    }
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

@Composable
fun rememberSteppedDegrees(epochSecond: Long, periodSeconds: Int, degreesPerStep: Float): Float {
    val step = epochSecond / periodSeconds
    val target = step * degreesPerStep
    val angle = remember { Animatable(target) }
    LaunchedEffect(step) {
        angle.animateTo(target, tween(durationMillis = 260, easing = FastOutSlowInEasing))
    }
    return angle.value
}

private const val RING_COUNT = 3
private const val SEGMENTS = 3
private const val RING_STROKE = 4f
private const val RING_CLEARANCE = 5f
private const val RING_PITCH = 14f
private const val GAP_DEGREES = 34f
