package com.coryblische.razrclock

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.NightMode
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.theme.RazrTheme
import com.coryblische.razrclock.ui.ClockScreen
import org.junit.Rule
import org.junit.Test

/**
 * Razr Ultra 2025 cover: 1080x1272 px at 360 dpi (480 x 565.33 dp).
 */
class CoverClockSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 1080,
            screenHeight = 1272,
            xdpi = 360,
            ydpi = 360,
            density = Density(360),
            nightMode = NightMode.NIGHT,
            softButtons = false,
        ),
        showSystemUi = false,
        useDeviceResolution = true,
        theme = "android:Theme.Material.NoActionBar.Fullscreen",
    )

    @Test
    fun coverClock() {
        paparazzi.snapshot {
            RazrTheme {
                ClockScreen(state = ClockFaceState.sample())
            }
        }
    }
}
