package com.coryblische.razrclock.ui

import android.content.Context
import android.content.ContextWrapper
import android.view.DisplayCutout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.data.accessibleClockLabel
import com.coryblische.razrclock.data.formatDate
import com.coryblische.razrclock.data.formatDayPeriod
import com.coryblische.razrclock.data.formatHoursMinutes
import com.coryblische.razrclock.data.formatSeconds
import com.coryblische.razrclock.display.CoverGeometry
import com.coryblische.razrclock.display.clockRegions
import com.coryblische.razrclock.posture.BatteryStatus
import com.coryblische.razrclock.posture.label
import com.coryblische.razrclock.theme.LocalBitcount
import com.coryblische.razrclock.theme.LocalPalette
import com.coryblische.razrclock.theme.RazrTheme
import com.coryblische.razrclock.theme.terminalTextStyle
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val PROMPT = "cory@razr ~ $ date"

@Composable
fun ClockScreen(state: ClockFaceState, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val font = LocalBitcount.current
    val density = LocalDensity.current
    val cutout = rememberDisplayCutout()
    var cursorOn by remember(state.cursorVisible) { mutableStateOf(state.cursorVisible) }
    LaunchedEffect(state.cursorVisible) {
        cursorOn = state.cursorVisible
        if (!state.cursorVisible) return@LaunchedEffect
        while (true) {
            delay(530)
            cursorOn = !cursorOn
        }
    }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(palette.darkerBackground)
            .semantics { contentDescription = accessibleClockLabel(state) },
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val margin = with(density) { 16.dp.toPx() }
        val promptHeight = with(density) { 46.dp.toPx() }
        val avoid = CoverGeometry.avoidRect(widthPx, heightPx, cutout)
        val regions = clockRegions(widthPx, heightPx, margin, promptHeight, avoid)
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawPath(regions.frame, palette.background)
                    drawRect(
                        color = palette.selection,
                        topLeft = Offset(regions.prompt.left, regions.prompt.top),
                        size = Size(regions.prompt.width, regions.prompt.height),
                    )
                    drawPath(
                        regions.frame,
                        palette.brown,
                        style = Stroke(width = with(density) { 2.5.dp.toPx() }),
                    )
                },
        ) {
            Box(Modifier.place(regions.prompt), contentAlignment = Alignment.CenterStart) {
                PromptLine(
                    fontSize = promptFontSize(regions.prompt.width, density),
                    cursorOn = cursorOn,
                )
            }
            Box(Modifier.place(regions.upper), contentAlignment = Alignment.Center) {
                TimeBlock(state)
            }
            Box(Modifier.place(regions.lower), contentAlignment = Alignment.BottomStart) {
                StatusBlock(state)
            }
        }
    }
}

private fun Modifier.place(rect: androidx.compose.ui.geometry.Rect): Modifier = layout { measurable, constraints ->
    val width = rect.width.roundToInt().coerceAtLeast(1)
    val height = rect.height.roundToInt().coerceAtLeast(1)
    val placeable = measurable.measure(Constraints.fixed(width, height))
    val parentW = if (constraints.hasBoundedWidth) constraints.maxWidth else width
    val parentH = if (constraints.hasBoundedHeight) constraints.maxHeight else height
    layout(parentW, parentH) {
        placeable.place(rect.left.roundToInt(), rect.top.roundToInt())
    }
}

@Composable
private fun promptFontSize(widthPx: Float, density: androidx.compose.ui.unit.Density): TextUnit {
    val glyphs = PROMPT.length + 1
    val px = (widthPx / (glyphs * 0.62f)).coerceIn(
        with(density) { 13.dp.toPx() },
        with(density) { 26.dp.toPx() },
    )
    return with(density) { px.toSp() }
}

@Composable
private fun PromptLine(fontSize: TextUnit, cursorOn: Boolean) {
    val palette = LocalPalette.current
    val font = LocalBitcount.current
    Row(
        modifier = Modifier.padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Text(
            text = buildAnnotatedString {
                fun piece(text: String, color: androidx.compose.ui.graphics.Color) {
                    withStyle(SpanStyle(color = color)) { append(text) }
                }
                piece("cory", palette.brightGreen)
                piece("@", palette.muted)
                piece("razr", palette.brightCyan)
                piece(" ", palette.foreground)
                piece("~", palette.brightYellow)
                piece(" $ ", palette.foreground)
                piece("date", palette.selectionForeground)
            },
            style = terminalTextStyle(font, fontSize, palette.foreground),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
        val cursorPx = with(LocalDensity.current) { fontSize.toPx() }
        Box(
            Modifier
                .padding(start = 3.dp)
                .size(
                    width = with(LocalDensity.current) { (cursorPx * 0.55f).toDp() },
                    height = with(LocalDensity.current) { (cursorPx * 0.72f).toDp() },
                )
                .background(if (cursorOn) palette.accent else palette.selection),
        )
    }
}

@Composable
private fun TimeBlock(state: ClockFaceState) {
    val palette = LocalPalette.current
    val font = LocalBitcount.current
    val density = LocalDensity.current
    val time = formatHoursMinutes(state)
    val seconds = formatSeconds(state)
    val period = formatDayPeriod(state)
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val maxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val maxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        var timePx = maxW / (time.length * 0.60f)
        val datePx = if (state.preferences.showDate) with(density) { 22.sp.toPx() } else 0f
        val secondsPx = timePx * if (seconds != null) 0.36f else 0f
        val periodExtra = if (period != null) timePx * 0.22f else 0f
        val used = datePx + timePx * 0.78f + secondsPx + with(density) { 12.dp.toPx() }
        if (used > maxH) timePx *= maxH / used
        if (timePx * time.length * 0.60f + periodExtra > maxW) {
            timePx = (maxW - periodExtra) / (time.length * 0.60f)
        }
        timePx = timePx.coerceIn(with(density) { 36.dp.toPx() }, with(density) { 220.dp.toPx() })
        val timeSize = with(density) { timePx.toSp() }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.preferences.showDate) {
                androidx.compose.material3.Text(
                    text = formatDate(state),
                    style = terminalTextStyle(font, 22.sp, palette.darkForeground),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                androidx.compose.material3.Text(
                    text = colorizedDigits(time, palette.brightForeground, palette.accent),
                    style = terminalTextStyle(font, timeSize, palette.brightForeground, FontWeight.W500),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
                if (period != null) {
                    androidx.compose.material3.Text(
                        text = period,
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                        style = terminalTextStyle(
                            font,
                            with(density) { (timePx * 0.28f).toSp() },
                            palette.brightYellow,
                            FontWeight.W500,
                        ),
                    )
                }
            }
            if (seconds != null) {
                androidx.compose.material3.Text(
                    text = ":$seconds",
                    style = terminalTextStyle(
                        font,
                        with(density) { (timePx * 0.36f).toSp() },
                        palette.accent,
                        FontWeight.W500,
                    ),
                )
            }
        }
    }
}

private fun colorizedDigits(
    text: String,
    digit: androidx.compose.ui.graphics.Color,
    colon: androidx.compose.ui.graphics.Color,
) = buildAnnotatedString {
    text.forEach { ch ->
        withStyle(SpanStyle(color = if (ch == ':') colon else digit)) { append(ch) }
    }
}

@Composable
private fun StatusBlock(state: ClockFaceState) {
    val palette = LocalPalette.current
    val font = LocalBitcount.current
    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
        if (state.preferences.showBattery) {
            val percentColor = when {
                state.batteryPercent in 0..15 && state.batteryStatus != BatteryStatus.CHARGING -> palette.brightRed
                else -> palette.brightForeground
            }
            val statusColor = when (state.batteryStatus) {
                BatteryStatus.CHARGING, BatteryStatus.FULL -> palette.brightGreen
                BatteryStatus.DISCHARGING -> palette.darkForeground
                BatteryStatus.UNKNOWN -> palette.muted
            }
            val statusWord = when (state.batteryStatus) {
                BatteryStatus.CHARGING -> "charging"
                BatteryStatus.FULL -> "full"
                BatteryStatus.DISCHARGING -> "discharging"
                BatteryStatus.UNKNOWN -> "unknown"
            }
            val percent = if (state.batteryPercent < 0) "--" else "${state.batteryPercent}%"
            androidx.compose.material3.Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = palette.muted)) { append("bat  ") }
                    withStyle(SpanStyle(color = percentColor)) { append(percent) }
                },
                style = terminalTextStyle(font, 18.sp, palette.foreground),
                maxLines = 1,
            )
            androidx.compose.material3.Text(
                text = statusWord,
                style = terminalTextStyle(font, 18.sp, statusColor),
                maxLines = 1,
            )
        }
        val angle = state.hingeAngleDegrees
        if (angle != null || state.posture != com.coryblische.razrclock.posture.DevicePosture.UNKNOWN) {
            val hinge = if (angle != null) "hinge ${angle.roundToInt()}°" else "hinge"
            androidx.compose.material3.Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = palette.muted)) { append(hinge) }
                    append("  ")
                    withStyle(SpanStyle(color = palette.darkForeground)) { append(state.posture.label()) }
                },
                style = terminalTextStyle(font, 18.sp, palette.darkForeground),
                maxLines = 1,
            )
        }
        if (state.standActive) {
            androidx.compose.material3.Text(
                text = "stand",
                style = terminalTextStyle(font, 18.sp, palette.brightMagenta, FontWeight.W500),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun rememberDisplayCutout(): DisplayCutout? {
    val context = LocalContext.current
    return remember(context) { context.findDisplayCutout() }
}

private fun Context.findDisplayCutout(): DisplayCutout? {
    runCatching { display?.cutout }.getOrNull()?.let { return it }
    val base = (this as? ContextWrapper)?.baseContext ?: return null
    if (base === this) return null
    return base.findDisplayCutout()
}

@Preview(name = "Cover 1080x1272 at 360dpi", widthDp = 480, heightDp = 565, showBackground = true, backgroundColor = 0xFF070301)
@Composable
private fun ClockCoverPreview() {
    RazrTheme {
        ClockScreen(ClockFaceState.sample())
    }
}
