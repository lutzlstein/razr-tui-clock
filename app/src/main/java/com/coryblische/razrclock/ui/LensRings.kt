package com.coryblische.razrclock.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * One toothed rim on each lens, plus the compound idler in the notch above
 * the tangent. Strokes only. At rest the ink is a dim, desaturated palette
 * color; [lit] is the 50 ms gesture flash at full color.
 */
fun DrawScope.drawGearTrain(
    left: PlacedLens,
    right: PlacedLens,
    epochSecond: Long,
    palette: OmarchyPalette,
    lit: Boolean,
) {
    val module = GearTrain.MODULE * ((left.radius / 110f) + (right.radius / 113f)) / 2f
    val pose = GearTrain.pose(epochSecond, left.center.x, left.center.y, right.center.x, right.center.y, module)
    val stroke = Stroke(width = 1.35f, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
    val rim = if (lit) palette.accent else palette.accent.resting()
    val wheel = if (lit) palette.brown else palette.brown.resting()
    val pinion = if (lit) palette.orange else palette.orange.resting()
    val rightRim = if (lit) palette.brightMagenta else palette.brightMagenta.resting()
    val marker = if (lit) palette.brightYellow else palette.brightYellow.resting(0.45f)
    val screen = Path().apply { addRect(Rect(0f, 0f, size.width, size.height)) }
    // The wheel and the right rim are in different planes of the compound.
    // Cut the wheel out of the right rim so those teeth never cross.
    val rightOuter = GearTrain.pitchRadius(GearTrain.TEETH_R, module) + module + 3f
    val rightDisk = Path().apply {
        addOval(
            Rect(
                right.center.x - rightOuter,
                right.center.y - rightOuter,
                right.center.x + rightOuter,
                right.center.y + rightOuter,
            ),
        )
    }
    val wheelWindow = Path.combine(PathOperation.Difference, screen, rightDisk)
    clipPath(wheelWindow) {
        drawGear(Offset(pose.idleX, pose.idleY), GearTrain.TEETH_WHEEL, module, pose.thetaWheel, wheel, stroke)
    }
    clipPath(screen) {
        drawGear(
            Offset(left.center.x, left.center.y),
            GearTrain.TEETH_L,
            module,
            pose.thetaL,
            rim,
            stroke,
            markerEvery = GearTrain.MARKER_EVERY,
            marker = marker,
        )
        drawGear(
            Offset(right.center.x, right.center.y),
            GearTrain.TEETH_R,
            module,
            pose.thetaR,
            rightRim,
            stroke,
            markerTooth = 0,
            marker = marker,
        )
        drawGear(Offset(pose.idleX, pose.idleY), GearTrain.TEETH_PINION, module, pose.thetaPinion, pinion, stroke)
        val index = if (lit) palette.brightYellow else palette.muted.resting(0.55f)
        drawIndex(
            Offset(left.center.x, left.center.y),
            GearTrain.pitchRadius(GearTrain.TEETH_L, module) + module,
            pose.indexAngle,
            index,
            stroke,
        )
    }
}

private fun DrawScope.drawGear(
    center: Offset,
    teeth: Int,
    module: Float,
    rotation: Float,
    color: Color,
    stroke: Stroke,
    markerEvery: Int = 0,
    markerTooth: Int = -1,
    marker: Color = color,
) {
    val pitchRadius = teeth * module / 2f
    val outer = pitchRadius + module
    val root = pitchRadius - module * 1.2f
    val pitch = 360f / teeth
    val tooth = pitch * 0.46f
    val path = Path()
    fun point(radius: Float, degrees: Float): Offset {
        val radians = Math.toRadians(degrees.toDouble())
        return Offset(
            center.x + cos(radians).toFloat() * radius,
            center.y + sin(radians).toFloat() * radius,
        )
    }
    val origin = point(root, rotation - tooth / 2f)
    path.moveTo(origin.x, origin.y)
    val outerRect = Rect(center.x - outer, center.y - outer, center.x + outer, center.y + outer)
    val rootRect = Rect(center.x - root, center.y - root, center.x + root, center.y + root)
    repeat(teeth) { index ->
        val mid = rotation + index * pitch
        val start = mid - tooth / 2f
        val end = mid + tooth / 2f
        val next = mid + pitch - tooth / 2f
        val flank = point(outer, start)
        path.lineTo(flank.x, flank.y)
        path.arcTo(outerRect, start, tooth, false)
        val rootPoint = point(root, end)
        path.lineTo(rootPoint.x, rootPoint.y)
        path.arcTo(rootRect, end, pitch - tooth, false)
    }
    path.close()
    drawPath(path, color, style = stroke)
    if (markerEvery > 0 || markerTooth >= 0) {
        repeat(teeth) { index ->
            val marked = (markerEvery > 0 && index % markerEvery == 0) || index == markerTooth
            if (!marked) return@repeat
            val mid = rotation + index * pitch
            val tip = Rect(center.x - outer, center.y - outer, center.x + outer, center.y + outer)
            drawArc(
                color = marker,
                startAngle = mid - tooth / 2f,
                sweepAngle = tooth,
                useCenter = false,
                topLeft = Offset(tip.left, tip.top),
                size = tip.size,
                style = stroke,
            )
        }
    }
}

private fun DrawScope.drawIndex(
    center: Offset,
    outer: Float,
    angle: Float,
    color: Color,
    stroke: Stroke,
) {
    val radians = Math.toRadians(angle.toDouble())
    val cosA = cos(radians).toFloat()
    val sinA = sin(radians).toFloat()
    drawLine(
        color,
        Offset(center.x + cosA * (outer + 3f), center.y + sinA * (outer + 3f)),
        Offset(center.x + cosA * (outer + 11f), center.y + sinA * (outer + 11f)),
        stroke.width,
        StrokeCap.Butt,
    )
}

private fun Color.resting(strength: Float = 0.72f): Color = Color(
    red = red * (1f - strength) + 0.09f * strength,
    green = green * (1f - strength) + 0.05f * strength,
    blue = blue * (1f - strength) + 0.025f * strength,
    alpha = 1f,
)

fun ringColors(palette: OmarchyPalette): List<Color> = listOf(
    palette.accent,
    palette.orange,
    palette.brown,
    palette.muted,
)
