package com.coryblische.razrclock

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
    fun defaultLensesAreSideBySideInsideTheCutout() {
        val lenses = LensPair()
        assertTrue(lenses.centersInsideCutout())
        assertEquals(lenses.ay, lenses.by)
        assertTrue(lenses.bx > lenses.ax)
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
