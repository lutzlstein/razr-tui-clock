package com.coryblische.razrclock

import com.coryblische.razrclock.data.CoverHoles
import com.coryblische.razrclock.data.LensPair
import com.coryblische.razrclock.data.WeatherNow
import com.coryblische.razrclock.data.formatRate
import com.coryblische.razrclock.data.formatSysmon
import com.coryblische.razrclock.data.formatWeatherLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundTwoTest {
    @Test
    fun ratesMatchTheSysmonExamples() {
        assertEquals("1.2 MB/s", formatRate(1_200_000.0))
        assertEquals("48 KB/s", formatRate(48_000.0))
        assertEquals("↓ 1.2 MB/s   ↑ 48 KB/s", formatSysmon(1_200_000.0, 48_000.0, ramPercent = null))
    }

    @Test
    fun measuredBezelsAreTheLensDefaults() {
        assertEquals(721f, CoverHoles.lensL.x, 0.15f)
        assertEquals(1090f, CoverHoles.lensL.y, 0.15f)
        assertEquals(110f, CoverHoles.lensL.radius, 0.15f)
        assertEquals(948f, CoverHoles.lensR.x, 0.15f)
        assertEquals(1092f, CoverHoles.lensR.y, 0.15f)
        assertEquals(113f, CoverHoles.lensR.radius, 0.15f)
        assertEquals(540f, CoverHoles.flash.x, 0.15f)
        assertEquals(1088f, CoverHoles.flash.y, 0.15f)
        assertEquals(34f, CoverHoles.flash.radius, 0.15f)
        val lenses = LensPair()
        assertEquals(CoverHoles.lensL.x, lenses.ax, 0.01f)
        assertTrue(lenses.centersInsideCutout())
    }

    @Test
    fun weatherLineUsesFahrenheitByDefault() {
        val line = formatWeatherLine(WeatherNow.sample(), fahrenheit = true)
        assertTrue(line.startsWith("72°F"))
        assertTrue(line.contains("fair"))
        assertTrue(line.contains("H 78"))
        assertTrue(line.contains("L 61"))
    }
}
