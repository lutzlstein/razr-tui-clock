package com.coryblische.razrclock

import com.coryblische.razrclock.data.CoverHoles
import com.coryblische.razrclock.data.LensPair
import com.coryblische.razrclock.data.WeatherNow
import com.coryblische.razrclock.data.upcomingHours
import java.time.LocalDateTime
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
    fun upcomingHoursSkipThePast() {
        val hours = upcomingHours(
            times = listOf("2026-11-09T20:00", "2026-11-09T21:00", "2026-11-09T22:00", "2026-11-09T23:00", "2026-11-10T00:00"),
            temps = listOf(18.0, 17.2, 16.4, 15.5, 15.0),
            codes = listOf(0, 1, 2, 3, 1),
            now = LocalDateTime.of(2026, 11, 9, 21, 40),
        )
        assertEquals(4, hours.size)
        assertEquals(21, hours.first().hour)
        assertEquals(0, hours.last().hour)
        assertEquals(17.2, hours.first().tempC, 0.01)
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
