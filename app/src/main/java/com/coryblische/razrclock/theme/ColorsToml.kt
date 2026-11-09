package com.coryblische.razrclock.theme

/**
 * Reads a flat `key = "#rrggbb"` palette. Comments start at the beginning of a line.
 * Quoted and bare hex values are both accepted. Unknown lines are ignored.
 */
object ColorsToml {
    val keys: List<String> = listOf(
        "accent",
        "selection",
        "selection_foreground",
        "muted",
        "background",
        "dark_background",
        "darker_background",
        "lighter_background",
        "foreground",
        "dark_foreground",
        "light_foreground",
        "bright_foreground",
        "red",
        "yellow",
        "orange",
        "green",
        "cyan",
        "blue",
        "magenta",
        "brown",
        "bright_red",
        "bright_yellow",
        "bright_green",
        "bright_cyan",
        "bright_blue",
        "bright_magenta",
    )

    private val assignment = Regex(
        """^([A-Za-z0-9_]+)\s*=\s*(?:"#([0-9A-Fa-f]{6})"|'#([0-9A-Fa-f]{6})'|#([0-9A-Fa-f]{6}))\s*$""",
    )

    fun parse(text: String): Map<String, Int> {
        val out = linkedMapOf<String, Int>()
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val match = assignment.matchEntire(line) ?: continue
            val key = match.groupValues[1]
            val hex = match.groupValues.drop(2).first { it.isNotEmpty() }
            val rgb = hex.toInt(16)
            out[key] = (0xFF shl 24) or rgb
        }
        return out
    }
}
