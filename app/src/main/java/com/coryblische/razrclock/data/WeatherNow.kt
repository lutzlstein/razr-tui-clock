package com.coryblische.razrclock.data

import kotlin.math.roundToInt

data class WeatherNow(
    val tempC: Double,
    val highC: Double,
    val lowC: Double,
    val code: Int,
    val fetchedAtMs: Long,
    val stale: Boolean,
    val place: String,
) {
    fun condition(): String = conditionLabel(code)

    companion object {
        fun sample(): WeatherNow = WeatherNow(
            tempC = 22.2,
            highC = 25.6,
            lowC = 16.1,
            code = 1,
            fetchedAtMs = 0L,
            stale = false,
            place = "New York",
        )
    }
}

fun conditionLabel(code: Int): String = when (code) {
    0 -> "clear"
    1, 2 -> "fair"
    3 -> "cloudy"
    45, 48 -> "fog"
    51, 53, 55, 56, 57 -> "drizzle"
    61, 63, 65, 66, 67 -> "rain"
    71, 73, 75, 77 -> "snow"
    80, 81, 82 -> "showers"
    85, 86 -> "snow"
    95, 96, 99 -> "storm"
    else -> "weather"
}

fun celsiusToFahrenheit(c: Double): Double = c * 9.0 / 5.0 + 32.0

fun formatTemperature(celsius: Double, fahrenheit: Boolean): String {
    val value = if (fahrenheit) celsiusToFahrenheit(celsius) else celsius
    val unit = if (fahrenheit) "F" else "C"
    return "${value.roundToInt()}°$unit"
}

fun formatWeatherLine(weather: WeatherNow, fahrenheit: Boolean): String {
    val unit = if (fahrenheit) "F" else "C"
    fun n(celsius: Double): Int = (if (fahrenheit) celsiusToFahrenheit(celsius) else celsius).roundToInt()
    val stale = if (weather.stale) "  stale" else ""
    return "${n(weather.tempC)}°$unit  ${weather.condition()}$stale   H ${n(weather.highC)}  L ${n(weather.lowC)}"
}
