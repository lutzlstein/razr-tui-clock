package com.coryblische.razrclock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.coryblische.razrclock.ui.rememberBitcount

val LocalPalette = staticCompositionLocalOf { OmarchyPalette.MaineAutumnSunset }
val LocalBitcount = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

fun terminalTextStyle(
    font: FontFamily,
    size: TextUnit,
    color: androidx.compose.ui.graphics.Color,
    weight: FontWeight = FontWeight.W400,
): TextStyle = TextStyle(
    fontFamily = font,
    fontWeight = weight,
    fontSize = size,
    color = color,
    lineHeight = size,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

@Composable
fun RazrTheme(
    palette: OmarchyPalette = OmarchyPalette.MaineAutumnSunset,
    content: @Composable () -> Unit,
) {
    val bitcount = rememberBitcount()
    val scheme = darkColorScheme(
        primary = palette.accent,
        onPrimary = palette.darkerBackground,
        secondary = palette.yellow,
        onSecondary = palette.darkerBackground,
        background = palette.darkerBackground,
        onBackground = palette.foreground,
        surface = palette.lighterBackground,
        onSurface = palette.foreground,
        surfaceVariant = palette.selection,
        onSurfaceVariant = palette.selectionForeground,
        outline = palette.muted,
        error = palette.brightRed,
        onError = palette.darkerBackground,
    )
    val type = Typography(
        bodyLarge = terminalTextStyle(bitcount, 18.sp, palette.foreground),
        bodyMedium = terminalTextStyle(bitcount, 15.sp, palette.foreground),
        bodySmall = terminalTextStyle(bitcount, 13.sp, palette.darkForeground),
        titleLarge = terminalTextStyle(bitcount, 28.sp, palette.brightForeground, FontWeight.W500),
        titleMedium = terminalTextStyle(bitcount, 20.sp, palette.foreground, FontWeight.W500),
        labelLarge = terminalTextStyle(bitcount, 16.sp, palette.darkerBackground, FontWeight.W500),
    )
    CompositionLocalProvider(LocalPalette provides palette, LocalBitcount provides bitcount) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
