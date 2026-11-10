package com.coryblische.razrclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.data.CoverHoles
import com.coryblische.razrclock.data.LensPair
import com.coryblische.razrclock.data.accessibleClockLabel
import com.coryblische.razrclock.data.formatDate
import com.coryblische.razrclock.data.formatDayPeriod
import com.coryblische.razrclock.data.formatHoursMinutes
import com.coryblische.razrclock.data.formatSeconds
import com.coryblische.razrclock.data.formatRate
import com.coryblische.razrclock.data.conditionLabel
import com.coryblische.razrclock.data.formatHourLabel
import com.coryblische.razrclock.data.formatTemperature
import com.coryblische.razrclock.display.CoverGeometry
import com.coryblische.razrclock.presence.CoverView
import com.coryblische.razrclock.posture.BatteryStatus
import com.coryblische.razrclock.posture.label
import com.coryblische.razrclock.theme.LocalBitcount
import com.coryblische.razrclock.theme.LocalMono
import com.coryblische.razrclock.theme.LocalPalette
import com.coryblische.razrclock.theme.RazrTheme
import com.coryblische.razrclock.theme.onWhite
import com.coryblische.razrclock.theme.terminalTextStyle
import java.time.ZoneOffset
import kotlin.math.roundToInt

/**
 * Motorola draws one icon row on the cover, not the whole corner.
 * About 70px tall, left side only, around y 1120–1190 on a 1080×1272 panel.
 */
object CoverIconRow {
    const val TOP = 1120f
    const val BOTTOM = 1190f
    const val RIGHT = 585f
}

@Composable
fun ClockScreen(
    state: ClockFaceState,
    modifier: Modifier = Modifier,
    calibrating: Boolean = false,
    showLensGuides: Boolean = false,
    onLongPress: () -> Unit = {},
    onLenses: (LensPair) -> Unit = {},
    onCalibrationDone: () -> Unit = {},
) {
    val palette = if (state.preferences.lensAlign) LocalPalette.current.onWhite() else LocalPalette.current
    val mono = LocalMono.current
    val bitcount = LocalBitcount.current
    var selected by remember { mutableIntStateOf(0) }
    var working by remember(calibrating) { mutableStateOf(state.preferences.lenses) }
    val lenses = if (calibrating) working else state.preferences.lenses
    val epoch = state.now.toEpochSecond(ZoneOffset.UTC)

    CompositionLocalProvider(LocalPalette provides palette) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(if (state.preferences.lensAlign) Color.White else palette.darkerBackground)
            .semantics { contentDescription = accessibleClockLabel(state) },
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val cover = CoverGeometry.isCoverAspect(widthPx, heightPx)
        val sx = widthPx / LensPair.REFERENCE_WIDTH
        val sy = heightPx / LensPair.REFERENCE_HEIGHT
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    if (state.preferences.lensAlign) {
                        drawAlignmentGrid()
                    }
                    val a = lenses.place(0, size.width, size.height)
                    val b = lenses.place(1, size.width, size.height)
                    drawGearTrain(a, b, epoch, palette, lit = state.gearLit)
                    if (state.preferences.lensAlign) {
                        drawBlackLensGuides(a, b)
                    }
                    if (showLensGuides) {
                        drawLensGuides(palette)
                    }
                    if (calibrating) {
                        val mark = if (selected == 0) a else b
                        drawCircle(palette.brightForeground, radius = 5f, center = mark.center)
                        drawCircle(
                            palette.accent,
                            radius = mark.radius,
                            center = mark.center,
                            style = Stroke(width = 2f),
                        )
                    }
                },
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(
                    top = if (calibrating) 78.dp else 16.dp,
                    // Cover: stop above the status block and the single Motorola icon row.
                    bottom = if (cover) 168.dp else 12.dp,
                )
                .pointerInput(calibrating) {
                    if (!calibrating) {
                        detectTapGestures(onLongPress = { onLongPress() })
                    }
                },
        ) {
            if (!calibrating) {
                StatusStrip(state)
            }
            if (state.preferences.showDate) {
                Text(
                    text = formatDate(state),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 4.dp),
                    style = terminalTextStyle(mono, 26.sp, palette.lightForeground, FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = if (state.coverView == CoverView.CLOCK) Alignment.Center else Alignment.TopCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimeBlock(state, compact = state.coverView != CoverView.CLOCK)
                    when (state.coverView) {
                        CoverView.WEATHER -> ForecastBlock(state)
                        CoverView.SYSMON -> SysmonBlock(state)
                        CoverView.CLOCK -> {
                            WeatherBlock(state)
                            if (state.handActive) HourlyLine(state)
                        }
                    }
                }
            }
            if (!cover) {
                Text(
                    text = hingeText(state) + if (state.standActive) "  stand" else "",
                    modifier = Modifier.padding(top = 16.dp),
                    style = terminalTextStyle(
                        mono,
                        20.sp,
                        if (state.standActive) palette.brightMagenta else palette.foreground,
                        FontWeight.Bold,
                    ),
                    maxLines = 1,
                )
            }
        }
        if (cover) {
            val iconTop = CoverIconRow.TOP * sy
            val iconBottom = CoverIconRow.BOTTOM * sy
            val columnRight = CoverIconRow.RIGHT * sx - 12f
            val left = 22f * sx
            val width = (columnRight - left).coerceAtLeast(40f)
            Text(
                text = hingeText(state),
                modifier = Modifier.pinAbove(iconTop - 10f * sy, left, width),
                style = terminalTextStyle(mono, 22.sp, palette.foreground, FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = if (state.standActive) "stand" else state.posture.label(),
                modifier = Modifier.pinBottomClear(
                    bottomPx = heightPx - 28f * sy,
                    minTopPx = iconBottom + 8f * sy,
                    leftPx = left,
                    maxWidthPx = width,
                ),
                style = terminalTextStyle(
                    mono,
                    22.sp,
                    if (state.standActive) palette.brightMagenta else palette.foreground,
                    FontWeight.Bold,
                ),
                maxLines = 1,
                softWrap = false,
            )
        }
        if (calibrating) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(selected, lenses) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            var next = lenses.moved(selected, pan.x / sx, pan.y / sy)
                            if (zoom != 1f) next = next.scaledRadius(selected, zoom)
                            working = next
                            onLenses(next)
                        }
                    },
            )
            CalibrationBar(
                selected = selected,
                lenses = lenses,
                onSelect = { selected = it },
                onRadius = { delta ->
                    val next = lenses.radiusDelta(selected, delta)
                    working = next
                    onLenses(next)
                },
                onReset = {
                    working = LensPair()
                    onLenses(LensPair())
                },
                onDone = onCalibrationDone,
            )
        }
    }
    }
}

private fun Modifier.pinAbove(bottomPx: Float, leftPx: Float, maxWidthPx: Float): Modifier = layout { measurable, constraints ->
    val maxW = maxWidthPx.roundToInt().coerceIn(1, constraints.maxWidth)
    val placeable = measurable.measure(Constraints(maxWidth = maxW))
    val top = (bottomPx - placeable.height).roundToInt().coerceAtLeast(0)
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(leftPx.roundToInt(), top)
    }
}

private fun Modifier.pinBottomClear(
    bottomPx: Float,
    minTopPx: Float,
    leftPx: Float,
    maxWidthPx: Float,
): Modifier = layout { measurable, constraints ->
    val maxW = maxWidthPx.roundToInt().coerceIn(1, constraints.maxWidth)
    val placeable = measurable.measure(Constraints(maxWidth = maxW))
    val top = (bottomPx - placeable.height).roundToInt().coerceAtLeast(minTopPx.roundToInt())
    layout(constraints.maxWidth, constraints.maxHeight) {
        placeable.place(leftPx.roundToInt(), top)
    }
}

private fun hingeText(state: ClockFaceState): String {
    val angle = state.hingeAngleDegrees?.roundToInt()
    return if (angle != null) "hinge $angle°" else state.posture.label()
}

private fun DrawScope.drawAlignmentGrid() {
    val sx = size.width / LensPair.REFERENCE_WIDTH
    val sy = size.height / LensPair.REFERENCE_HEIGHT
    val left = (CoverHoles.CUTOUT_LEFT - 50f) * sx
    val top = (CoverHoles.CUTOUT_TOP - 50f) * sy
    val right = size.width
    val bottom = size.height
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        isAntiAlias = true
        textSize = 18f * sy
        typeface = android.graphics.Typeface.MONOSPACE
    }
    var x = 0f
    while (x <= LensPair.REFERENCE_WIDTH) {
        val px = x * sx
        if (px in left..right) {
            val major = x % 100f == 0f
            drawLine(
                Color.Black.copy(alpha = if (major) 0.35f else 0.16f),
                Offset(px, top.coerceAtLeast(0f)),
                Offset(px, bottom),
                strokeWidth = if (major) 1.5f else 1f,
            )
            if (major) {
                paint.alpha = 170
                drawContext.canvas.nativeCanvas.drawText(
                    x.toInt().toString(),
                    px + 3f,
                    (top + 16f * sy).coerceAtLeast(16f),
                    paint,
                )
            }
        }
        x += 50f
    }
    var y = 0f
    while (y <= LensPair.REFERENCE_HEIGHT) {
        val py = y * sy
        if (py in top..bottom) {
            val major = y % 100f == 0f
            drawLine(
                Color.Black.copy(alpha = if (major) 0.35f else 0.16f),
                Offset(left.coerceAtLeast(0f), py),
                Offset(right, py),
                strokeWidth = if (major) 1.5f else 1f,
            )
            if (major) {
                paint.alpha = 170
                drawContext.canvas.nativeCanvas.drawText(
                    y.toInt().toString(),
                    left.coerceAtLeast(2f),
                    py - 3f,
                    paint,
                )
            }
        }
        y += 50f
    }
}

private fun DrawScope.drawBlackLensGuides(left: PlacedLens, right: PlacedLens) {
    fun mark(lens: PlacedLens) {
        drawCircle(Color.Black, radius = lens.radius, center = lens.center, style = Stroke(width = 2f))
        val arm = 14f
        drawLine(Color.Black, Offset(lens.center.x - arm, lens.center.y), Offset(lens.center.x + arm, lens.center.y), 2f)
        drawLine(Color.Black, Offset(lens.center.x, lens.center.y - arm), Offset(lens.center.x, lens.center.y + arm), 2f)
    }
    mark(left)
    mark(right)
}

private fun DrawScope.drawLensGuides(palette: com.coryblische.razrclock.theme.OmarchyPalette) {
    val sx = size.width / LensPair.REFERENCE_WIDTH
    val sy = size.height / LensPair.REFERENCE_HEIGHT
    val left = CoverHoles.CUTOUT_LEFT * sx
    val top = CoverHoles.CUTOUT_TOP * sy
    val right = (CoverHoles.CUTOUT_LEFT + CoverHoles.CUTOUT_WIDTH) * sx
    val bottom = (CoverHoles.CUTOUT_TOP + CoverHoles.CUTOUT_HEIGHT) * sy
    drawRect(
        color = palette.brightCyan,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        style = Stroke(width = 2f),
    )
    fun hole(h: CoverHoles.Hole, color: Color) {
        drawCircle(
            color = color,
            radius = h.radius * ((sx + sy) / 2f),
            center = Offset(h.x * sx, h.y * sy),
            style = Stroke(width = 2.5f),
        )
    }
    hole(CoverHoles.lensL, palette.brightYellow)
    hole(CoverHoles.lensR, palette.brightYellow)
    hole(CoverHoles.flash, palette.darkForeground)
}

@Composable
private fun CalibrationBar(
    selected: Int,
    lenses: LensPair,
    onSelect: (Int) -> Unit,
    onRadius: (Float) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    val palette = LocalPalette.current
    val mono = LocalMono.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(palette.background)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CalButton("1", selected == 0, Modifier.weight(1f)) { onSelect(0) }
            CalButton("2", selected == 1, Modifier.weight(1f)) { onSelect(1) }
            CalButton("−", false, Modifier.weight(1f)) { onRadius(-4f) }
            CalButton("+", false, Modifier.weight(1f)) { onRadius(4f) }
            CalButton("defaults", false, Modifier.weight(1.6f)) { onReset() }
            CalButton("done", false, Modifier.weight(1.4f)) { onDone() }
        }
        Text(
            text = "${selected + 1}  ${lenses.x(selected).roundToInt()},${lenses.y(selected).roundToInt()}  r${lenses.r(selected).roundToInt()}",
            modifier = Modifier.padding(top = 4.dp, start = 6.dp),
            style = terminalTextStyle(mono, 16.sp, palette.lightForeground, FontWeight.Bold),
        )
    }
}

@Composable
private fun CalButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val mono = LocalMono.current
    Box(
        modifier
            .height(52.dp)
            .background(if (selected) palette.accent else palette.lighterBackground)
            .pointerInput(onClick) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = terminalTextStyle(
                mono,
                18.sp,
                if (selected) palette.darkerBackground else palette.foreground,
                FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun TimeBlock(state: ClockFaceState, compact: Boolean = false) {
    val palette = LocalPalette.current
    val bitcount = LocalBitcount.current
    val mono = LocalMono.current
    val time = formatHoursMinutes(state)
    val seconds = formatSeconds(state)
    val period = formatDayPeriod(state)
    val digits = if (compact) 84.sp else 108.sp
    val secondSize = if (compact) 30.sp else 40.sp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = colorizedDigits(time, palette.brightForeground, palette.accent),
                style = terminalTextStyle(bitcount, digits, palette.brightForeground, FontWeight.W500),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
            if (period != null) {
                Text(
                    text = period,
                    modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
                    style = terminalTextStyle(mono, 28.sp, palette.brightYellow, FontWeight.Bold),
                )
            }
        }
        if (seconds != null) {
            Text(
                text = ":$seconds",
                style = terminalTextStyle(bitcount, secondSize, palette.accent, FontWeight.W500),
            )
        }
    }
}

private fun colorizedDigits(
    text: String,
    digit: Color,
    colon: Color,
) = buildAnnotatedString {
    text.forEach { ch ->
        withStyle(SpanStyle(color = if (ch == ':') colon else digit)) { append(ch) }
    }
}

@Composable
private fun StatusStrip(state: ClockFaceState) {
    val palette = LocalPalette.current
    val mono = LocalMono.current
    val style = terminalTextStyle(mono, 18.sp, palette.darkForeground, FontWeight.Bold)
    val percent = if (state.batteryPercent < 0) "--" else "${state.batteryPercent}%"
    val charging = state.batteryStatus == BatteryStatus.CHARGING || state.batteryStatus == BatteryStatus.FULL
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("↓ ${formatRate(state.downBytesPerSec)}", style = style, maxLines = 1)
        Text("↑ ${formatRate(state.upBytesPerSec)}", style = style, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(percent, style = style, maxLines = 1)
            if (charging) {
                Box(
                    Modifier
                        .padding(start = 4.dp)
                        .size(14.dp)
                        .drawBehind {
                            val bolt = palette.brightGreen
                            drawLine(bolt, Offset(size.width * 0.55f, 0f), Offset(size.width * 0.28f, size.height * 0.52f), 2.4f)
                            drawLine(bolt, Offset(size.width * 0.28f, size.height * 0.52f), Offset(size.width * 0.62f, size.height * 0.52f), 2.4f)
                            drawLine(bolt, Offset(size.width * 0.62f, size.height * 0.52f), Offset(size.width * 0.38f, size.height), 2.4f)
                        },
                )
            }
        }
        state.ramPercent?.let { ram ->
            Text("ram $ram%", style = style, maxLines = 1)
        }
    }
}

@Composable
private fun WeatherBlock(state: ClockFaceState) {
    val palette = LocalPalette.current
    val mono = LocalMono.current
    val weather = state.weather
    val fahrenheit = state.preferences.useFahrenheit
    Column(
        Modifier.padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (weather == null) {
            Text(
                text = "weather --",
                style = terminalTextStyle(mono, 28.sp, palette.foreground, FontWeight.Bold),
            )
            return
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            WeatherGlyph(weather.code, Modifier.padding(end = 10.dp).size(36.dp))
            Text(
                text = formatTemperature(weather.tempC, fahrenheit),
                style = terminalTextStyle(mono, 34.sp, palette.brightForeground, FontWeight.Bold),
            )
            Text(
                text = "  ${weather.condition()}",
                style = terminalTextStyle(mono, 28.sp, palette.lightForeground, FontWeight.Bold),
            )
        }
        val high = formatTemperature(weather.highC, fahrenheit).substringBefore("°")
        val low = formatTemperature(weather.lowC, fahrenheit).substringBefore("°")
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = palette.darkForeground)) { append("H $high    L $low") }
                if (weather.stale) {
                    withStyle(SpanStyle(color = palette.brightYellow)) { append("   stale") }
                }
            },
            modifier = Modifier.padding(top = 2.dp),
            style = terminalTextStyle(mono, 16.sp, palette.darkForeground, FontWeight.Bold),
        )
    }
}

@Composable
private fun WeatherGlyph(code: Int?, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier.drawBehind {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension / 2f
            when (code) {
                null -> drawLine(palette.darkForeground, Offset(r * 0.4f, c.y), Offset(size.width - r * 0.4f, c.y), 3f)
                0, 1 -> {
                    drawCircle(palette.brightYellow, r * 0.45f, c)
                }
                2, 3, 45, 48 -> drawCircle(palette.darkForeground, r * 0.55f, c, style = Stroke(3f))
                71, 73, 75, 77, 85, 86 -> drawCircle(palette.brightForeground, r * 0.28f, c)
                95, 96, 99 -> drawLine(palette.brightYellow, Offset(c.x, r * 0.2f), Offset(c.x - r * 0.25f, size.height), 3f)
                else -> drawLine(palette.brightCyan, Offset(c.x, r * 0.15f), Offset(c.x, size.height * 0.85f), 3f)
            }
        },
    )
}

@Composable
private fun HourlyLine(state: ClockFaceState) {
    val hours = state.weather?.hours.orEmpty()
    if (hours.isEmpty()) return
    val palette = LocalPalette.current
    val mono = LocalMono.current
    val use24 = state.preferences.use24Hour
    val fahrenheit = state.preferences.useFahrenheit
    Text(
        text = hours.joinToString("   ") { hour ->
            "${formatHourLabel(hour.hour, use24)} ${formatTemperature(hour.tempC, fahrenheit)}"
        },
        modifier = Modifier.padding(top = 8.dp),
        style = terminalTextStyle(mono, 16.sp, palette.lightForeground, FontWeight.Bold),
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ForecastBlock(state: ClockFaceState) {
    val palette = LocalPalette.current
    val mono = LocalMono.current
    val weather = state.weather
    val use24 = state.preferences.use24Hour
    val fahrenheit = state.preferences.useFahrenheit
    Column(
        Modifier.padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "forecast",
            style = terminalTextStyle(mono, 13.sp, palette.darkForeground, FontWeight.Bold),
        )
        if (weather == null || weather.hours.isEmpty()) {
            Text(
                text = "hourly --",
                modifier = Modifier.padding(top = 2.dp),
                style = terminalTextStyle(mono, 18.sp, palette.foreground, FontWeight.Bold),
            )
            return
        }
        // Three rows fit above the lens rings. A fourth was clipped on the cover.
        weather.hours.take(3).forEach { hour ->
            Text(
                text = "${formatHourLabel(hour.hour, use24)}   ${formatTemperature(hour.tempC, fahrenheit)}   ${conditionLabel(hour.code)}",
                style = terminalTextStyle(mono, 18.sp, palette.foreground, FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Composable
private fun SysmonBlock(state: ClockFaceState) {
    val palette = LocalPalette.current
    val mono = LocalMono.current
    val style = terminalTextStyle(mono, 20.sp, palette.brightForeground, FontWeight.Bold)
    val dim = terminalTextStyle(mono, 14.sp, palette.darkForeground, FontWeight.Bold)
    val percent = if (state.batteryPercent < 0) "--" else "${state.batteryPercent}%"
    val charging = state.batteryStatus == BatteryStatus.CHARGING || state.batteryStatus == BatteryStatus.FULL
    Column(
        Modifier.padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("sysmon", style = dim)
        Text("↓ ${formatRate(state.downBytesPerSec)}", modifier = Modifier.padding(top = 4.dp), style = style, maxLines = 1)
        Text("↑ ${formatRate(state.upBytesPerSec)}", style = style, maxLines = 1)
        Text(
            text = "ram ${state.ramPercent?.let { "$it%" } ?: "--"}",
            style = style,
            maxLines = 1,
        )
        Text(
            text = "$percent${if (charging) "  charging" else ""}",
            style = terminalTextStyle(mono, 22.sp, palette.brightGreen, FontWeight.Bold),
            maxLines = 1,
        )
    }
}

@Preview(name = "Cover 1080x1272 at 360dpi", widthDp = 480, heightDp = 565, showBackground = true, backgroundColor = 0xFF070301)
@Composable
private fun ClockCoverPreview() {
    RazrTheme {
        ClockScreen(ClockFaceState.sample())
    }
}
