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
 * Nested circular maze. Each ring is broken into arcs with gaps, and each arc
 * ends in a short radial tick so the joint reads as a right-angle turn, the
 * same idea as the square labyrinth in the Omarchy mark.
 */
fun DrawScope.drawMazeRings(
    lens: PlacedLens,
    rotationDegrees: Float,
    colors: List<Color>,
    emphasized: Boolean,
) {
    val stroke = if (emphasized) 5.5f else 4f
    repeat(RING_COUNT) { ring ->
        val radius = lens.radius * (1.16f + ring * 0.30f)
        val color = colors[ring % colors.size]
        val gap = 28f + ring * 6f
        val sweep = (360f / SEGMENTS) - gap
        val base = rotationDegrees + ring * 19f
        repeat(SEGMENTS) { segment ->
            val start = base + segment * (360f / SEGMENTS)
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(lens.center.x - radius, lens.center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
            val end = Math.toRadians((start + sweep).toDouble())
            val turn = if ((ring + segment) % 2 == 0) 14f else -14f
            val x1 = lens.center.x + cos(end).toFloat() * radius
            val y1 = lens.center.y + sin(end).toFloat() * radius
            val x2 = lens.center.x + cos(end).toFloat() * (radius + turn)
            val y2 = lens.center.y + sin(end).toFloat() * (radius + turn)
            drawLine(
                color = color,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = stroke,
                cap = StrokeCap.Square,
            )
        }
    }
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

private const val RING_COUNT = 4
private const val SEGMENTS = 3
