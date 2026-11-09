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
import com.coryblische.razrclock.data.LensPair
import com.coryblische.razrclock.data.accessibleClockLabel
import com.coryblische.razrclock.data.formatDate
import com.coryblische.razrclock.data.formatDayPeriod
import com.coryblische.razrclock.data.formatHoursMinutes
import com.coryblische.razrclock.data.formatSeconds
import com.coryblische.razrclock.data.formatSysmon
import com.coryblische.razrclock.data.formatWeatherLine
import com.coryblische.razrclock.display.CoverGeometry
import com.coryblische.razrclock.posture.BatteryStatus
import com.coryblische.razrclock.posture.label
import com.coryblische.razrclock.theme.LocalBitcount
import com.coryblische.razrclock.theme.LocalMono
import com.coryblische.razrclock.theme.LocalPalette
import com.coryblische.razrclock.theme.RazrTheme
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
    onLongPress: () -> Unit = {},
    onLenses: (LensPair) -> Unit = {},
    onCalibrationDone: () -> Unit = {},
) {
    val palette = LocalPalette.current
    val mono = LocalMono.current
    val bitcount = LocalBitcount.current
    var selected by remember { mutableIntStateOf(0) }
    var working by remember(calibrating) { mutableStateOf(state.preferences.lenses) }
    val lenses = if (calibrating) working else state.preferences.lenses
    val epoch = state.now.toEpochSecond(ZoneOffset.UTC)
    val fast = rememberSteppedDegrees(epoch, periodSeconds = 1, degreesPerStep = 15f)
    val slow = rememberSteppedDegrees(epoch, periodSeconds = 15, degreesPerStep = 12f)
    val colors = ringColors(palette)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(palette.darkerBackground)
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
                    val a = lenses.place(0, size.width, size.height)
                    val b = lenses.place(1, size.width, size.height)
                    drawMazeRings(a, fast, colors, emphasized = calibrating && selected == 0)
                    drawMazeRings(b, slow, colors, emphasized = calibrating && selected == 1)
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
                Text(
                    text = formatSysmon(state.downBytesPerSec, state.upBytesPerSec, ramPercent = null),
                    style = terminalTextStyle(mono, 24.sp, palette.foreground, FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.preferences.showDate) {
                Text(
                    text = formatDate(state),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    style = terminalTextStyle(mono, 28.sp, palette.lightForeground, FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimeBlock(state)
                    WeatherRow(state)
                }
            }
            if (!cover) {
                StatusColumn(state, Modifier.padding(top = 12.dp, bottom = 8.dp))
            }
        }
        if (cover) {
            val iconTop = CoverIconRow.TOP * sy
            val iconBottom = CoverIconRow.BOTTOM * sy
            val columnRight = CoverIconRow.RIGHT * sx - 16f
            Box(
                Modifier.placeAbs(
                    left = 20f * sx,
                    top = iconTop - 168f * sy,
                    width = (columnRight - 20f * sx).coerceAtLeast(40f),
                    height = 160f * sy,
                ),
                contentAlignment = Alignment.BottomStart,
            ) {
                StatusColumn(state, hingeLine = false)
            }
            Box(
                Modifier.placeAbs(
                    left = 20f * sx,
                    top = iconBottom + 8f * sy,
                    width = (columnRight - 20f * sx).coerceAtLeast(40f),
                    height = (heightPx - iconBottom - 14f * sy).coerceAtLeast(20f),
                ),
                contentAlignment = Alignment.TopStart,
            ) {
                StatusColumn(state, batteryLines = false)
            }
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

private fun Modifier.placeAbs(left: Float, top: Float, width: Float, height: Float): Modifier = layout { measurable, _ ->
    val w = width.roundToInt().coerceAtLeast(1)
    val h = height.roundToInt().coerceAtLeast(1)
    val placeable = measurable.measure(Constraints.fixed(w, h))
    layout(w, h) {
        placeable.place(left.roundToInt(), top.roundToInt())
    }
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
            CalButton("reset", false, Modifier.weight(1.4f)) { onReset() }
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
private fun TimeBlock(state: ClockFaceState) {
    val palette = LocalPalette.current
    val bitcount = LocalBitcount.current
    val mono = LocalMono.current
    val time = formatHoursMinutes(state)
    val seconds = formatSeconds(state)
    val period = formatDayPeriod(state)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = colorizedDigits(time, palette.brightForeground, palette.accent),
                style = terminalTextStyle(bitcount, 108.sp, palette.brightForeground, FontWeight.W500),
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
                style = terminalTextStyle(bitcount, 40.sp, palette.accent, FontWeight.W500),
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
private fun WeatherRow(state: ClockFaceState) {
    val palette = LocalPalette.current
    val mono = LocalMono.current
    val weather = state.weather
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        WeatherGlyph(weather?.code, Modifier.padding(end = 8.dp).size(28.dp))
        Text(
            text = weather?.let { formatWeatherLine(it, state.preferences.useFahrenheit) } ?: "weather --",
            style = terminalTextStyle(
                mono,
                22.sp,
                if (weather?.stale == true) palette.brightYellow else palette.foreground,
                FontWeight.Bold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
private fun StatusColumn(
    state: ClockFaceState,
    modifier: Modifier = Modifier,
    batteryLines: Boolean = true,
    hingeLine: Boolean = true,
) {
    val palette = LocalPalette.current
    val mono = LocalMono.current
    Column(modifier) {
        if (batteryLines && state.preferences.showBattery) {
            val percent = if (state.batteryPercent < 0) "--" else "${state.batteryPercent}%"
            val charging = when (state.batteryStatus) {
                BatteryStatus.CHARGING -> "charging"
                BatteryStatus.FULL -> "full"
                BatteryStatus.DISCHARGING -> "discharging"
                BatteryStatus.UNKNOWN -> "unknown"
            }
            val chargingColor = when (state.batteryStatus) {
                BatteryStatus.CHARGING, BatteryStatus.FULL -> palette.brightGreen
                else -> palette.foreground
            }
            Text(
                text = "bat  $percent",
                style = terminalTextStyle(mono, 22.sp, palette.foreground, FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                text = charging,
                style = terminalTextStyle(mono, 22.sp, chargingColor, FontWeight.Bold),
                maxLines = 1,
            )
            state.ramPercent?.let { ram ->
                Text(
                    text = "ram  $ram%",
                    style = terminalTextStyle(mono, 22.sp, palette.foreground, FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
        if (hingeLine) {
            val angle = state.hingeAngleDegrees?.roundToInt()
            val hinge = if (angle != null) "hinge $angle°" else state.posture.label()
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = palette.foreground)) { append(hinge) }
                    if (state.standActive) {
                        withStyle(SpanStyle(color = palette.brightMagenta)) { append("  stand") }
                    } else {
                        withStyle(SpanStyle(color = palette.foreground)) { append("  ${state.posture.label()}") }
                    }
                },
                style = terminalTextStyle(mono, 22.sp, palette.foreground, FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

@Preview(name = "Cover 1080x1272 at 360dpi", widthDp = 480, heightDp = 565, showBackground = true, backgroundColor = 0xFF070301)
@Composable
private fun ClockCoverPreview() {
    RazrTheme {
        ClockScreen(ClockFaceState.sample())
    }
}
