package com.coryblische.razrclock

import com.coryblische.razrclock.theme.ColorsToml
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class ColorsTomlTest {
    @Test
    fun parsesQuotedHexAndSkipsComments() {
        val parsed = ColorsToml.parse(
            """
            # comment
            accent = "#c15a3c"
            muted = '#625c58'
            blue = #36a3c8
            not a line
            """.trimIndent(),
        )
        assertEquals(0xFFC15A3C.toInt(), parsed["accent"])
        assertEquals(0xFF625C58.toInt(), parsed["muted"])
        assertEquals(0xFF36A3C8.toInt(), parsed["blue"])
        assertEquals(3, parsed.size)
    }

    @Test
    fun bundledFileHasEveryKey() {
        val text = File("src/main/assets/colors.toml").readText()
        val parsed = ColorsToml.parse(text)
        assertEquals(ColorsToml.keys.toSet(), parsed.keys)
        assertEquals(0xFFC15A3C.toInt(), parsed["accent"])
        assertEquals(0xFF070301.toInt(), parsed["darker_background"])
        assertEquals(0xFFFFA83B.toInt(), parsed["bright_magenta"])
    }
}
