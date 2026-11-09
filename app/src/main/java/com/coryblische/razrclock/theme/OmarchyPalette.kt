package com.coryblische.razrclock.theme

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.coryblische.razrclock.RazrClockApp
import java.io.File

data class OmarchyPalette(
    val accent: Color,
    val selection: Color,
    val selectionForeground: Color,
    val muted: Color,
    val background: Color,
    val darkBackground: Color,
    val darkerBackground: Color,
    val lighterBackground: Color,
    val foreground: Color,
    val darkForeground: Color,
    val lightForeground: Color,
    val brightForeground: Color,
    val red: Color,
    val yellow: Color,
    val orange: Color,
    val green: Color,
    val cyan: Color,
    val blue: Color,
    val magenta: Color,
    val brown: Color,
    val brightRed: Color,
    val brightYellow: Color,
    val brightGreen: Color,
    val brightCyan: Color,
    val brightBlue: Color,
    val brightMagenta: Color,
) {
    companion object {
        val MaineAutumnSunset = OmarchyPalette(
            accent = Color(0xFFC15A3C),
            selection = Color(0xFF502319),
            selectionForeground = Color(0xFFD48460),
            muted = Color(0xFF625C58),
            background = Color(0xFF0E0502),
            darkBackground = Color(0xFF0B0402),
            darkerBackground = Color(0xFF070301),
            lighterBackground = Color(0xFF261E1B),
            foreground = Color(0xFFE7D3CA),
            darkForeground = Color(0xFFAD9E98),
            lightForeground = Color(0xFFEBDAD2),
            brightForeground = Color(0xFFEDDED7),
            red = Color(0xFFC15A3C),
            yellow = Color(0xFFD48460),
            orange = Color(0xFFCA7359),
            green = Color(0xFF3D8C50),
            cyan = Color(0xFF00B0F4),
            blue = Color(0xFF36A3C8),
            magenta = Color(0xFFF48737),
            brown = Color(0xFF794535),
            brightRed = Color(0xFFF07B53),
            brightYellow = Color(0xFFFFA778),
            brightGreen = Color(0xFF57B76D),
            brightCyan = Color(0xFF39D6FF),
            brightBlue = Color(0xFF56CAF9),
            brightMagenta = Color(0xFFFFA83B),
        )

        fun fromToml(text: String, base: OmarchyPalette = MaineAutumnSunset): OmarchyPalette {
            val parsed = ColorsToml.parse(text)
            fun pick(key: String, fallback: Color): Color = parsed[key]?.let { Color(it) } ?: fallback
            return base.copy(
                accent = pick("accent", base.accent),
                selection = pick("selection", base.selection),
                selectionForeground = pick("selection_foreground", base.selectionForeground),
                muted = pick("muted", base.muted),
                background = pick("background", base.background),
                darkBackground = pick("dark_background", base.darkBackground),
                darkerBackground = pick("darker_background", base.darkerBackground),
                lighterBackground = pick("lighter_background", base.lighterBackground),
                foreground = pick("foreground", base.foreground),
                darkForeground = pick("dark_foreground", base.darkForeground),
                lightForeground = pick("light_foreground", base.lightForeground),
                brightForeground = pick("bright_foreground", base.brightForeground),
                red = pick("red", base.red),
                yellow = pick("yellow", base.yellow),
                orange = pick("orange", base.orange),
                green = pick("green", base.green),
                cyan = pick("cyan", base.cyan),
                blue = pick("blue", base.blue),
                magenta = pick("magenta", base.magenta),
                brown = pick("brown", base.brown),
                brightRed = pick("bright_red", base.brightRed),
                brightYellow = pick("bright_yellow", base.brightYellow),
                brightGreen = pick("bright_green", base.brightGreen),
                brightCyan = pick("bright_cyan", base.brightCyan),
                brightBlue = pick("bright_blue", base.brightBlue),
                brightMagenta = pick("bright_magenta", base.brightMagenta),
            )
        }
    }
}

data class LoadedPalette(
    val palette: OmarchyPalette,
    val source: String,
)

fun loadPalette(context: Context): LoadedPalette {
    val external = File(context.getExternalFilesDir(null), "colors.toml")
    if (external.isFile) {
        val parsed = runCatching { OmarchyPalette.fromToml(external.readText()) }
        parsed.onSuccess { return LoadedPalette(it, "external colors.toml") }
        parsed.onFailure { Log.w(RazrClockApp.TAG, "Ignoring ${external.path}", it) }
    }
    val bundled = runCatching {
        context.assets.open("colors.toml").bufferedReader().use { it.readText() }
    }.getOrNull()
    if (bundled != null) {
        val parsed = runCatching { OmarchyPalette.fromToml(bundled) }
        parsed.onSuccess { return LoadedPalette(it, "bundled colors.toml") }
        parsed.onFailure { Log.w(RazrClockApp.TAG, "Ignoring bundled colors.toml", it) }
    }
    return LoadedPalette(OmarchyPalette.MaineAutumnSunset, "hardcoded Maine Autumn Sunset")
}
