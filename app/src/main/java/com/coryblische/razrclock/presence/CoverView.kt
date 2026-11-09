package com.coryblische.razrclock.presence

enum class CoverView {
    CLOCK,
    WEATHER,
    SYSMON,
    ;

    fun next(): CoverView = when (this) {
        CLOCK -> WEATHER
        WEATHER -> SYSMON
        SYSMON -> CLOCK
    }
}
