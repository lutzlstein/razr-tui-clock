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
import com.coryblische.razrclock.ui.rememberJetBrainsMono

val LocalPalette = staticCompositionLocalOf { OmarchyPalette.MaineAutumnSunset }
val LocalBitcount = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }
val LocalMono = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

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
    val mono = rememberJetBrainsMono()
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
        bodyLarge = terminalTextStyle(mono, 18.sp, palette.foreground, FontWeight.Bold),
        bodyMedium = terminalTextStyle(mono, 16.sp, palette.foreground, FontWeight.Bold),
        bodySmall = terminalTextStyle(mono, 14.sp, palette.darkForeground, FontWeight.Bold),
        titleLarge = terminalTextStyle(mono, 28.sp, palette.brightForeground, FontWeight.Bold),
        titleMedium = terminalTextStyle(mono, 20.sp, palette.foreground, FontWeight.Bold),
        labelLarge = terminalTextStyle(mono, 16.sp, palette.darkerBackground, FontWeight.Bold),
    )
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalBitcount provides bitcount,
        LocalMono provides mono,
    ) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
