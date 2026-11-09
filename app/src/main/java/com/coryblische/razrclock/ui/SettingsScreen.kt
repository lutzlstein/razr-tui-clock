package com.coryblische.razrclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coryblische.razrclock.R
import com.coryblische.razrclock.data.ClockPreferences
import com.coryblische.razrclock.theme.LocalMono
import com.coryblische.razrclock.theme.LocalPalette
import com.coryblische.razrclock.theme.terminalTextStyle

@Composable
fun SettingsScreen(
    prefs: ClockPreferences,
    paletteSource: String,
    diagnostics: String,
    onChange: (ClockPreferences) -> Unit,
    onPreview: () -> Unit,
    onShowCover: () -> Unit,
    onRearTransfer: () -> Unit,
    onDreamSettings: () -> Unit,
    onFullScreenIntent: () -> Unit,
    onBatterySettings: () -> Unit,
    onNotificationSettings: () -> Unit,
    onCalibrate: () -> Unit,
    onRequestLocation: () -> Unit,
) {
    val palette = LocalPalette.current
    val font = LocalMono.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.darkerBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "cory@razr ~ $ settings",
            style = terminalTextStyle(font, 22.sp, palette.brightGreen, FontWeight.W500),
        )
        Text(
            text = "maine autumn sunset  ·  $paletteSource",
            style = terminalTextStyle(font, 14.sp, palette.darkForeground),
        )
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.settings_intro),
            style = terminalTextStyle(font, 15.sp, palette.foreground),
        )
        HorizontalDivider(color = palette.brown)
        ToggleRow("24-hour time", prefs.use24Hour) { onChange(prefs.copy(use24Hour = it)) }
        ToggleRow("Show seconds", prefs.showSeconds) { onChange(prefs.copy(showSeconds = it)) }
        ToggleRow("Show date", prefs.showDate) { onChange(prefs.copy(showDate = it)) }
        ToggleRow("Show battery", prefs.showBattery) { onChange(prefs.copy(showBattery = it)) }
        ToggleRow("Stand mode when half folded", prefs.standMode) { onChange(prefs.copy(standMode = it)) }
        ToggleRow("Only while charging", prefs.standOnlyWhileCharging) {
            onChange(prefs.copy(standOnlyWhileCharging = it))
        }
        ToggleRow("Temperature in °F", prefs.useFahrenheit) { onChange(prefs.copy(useFahrenheit = it)) }
        ToggleRow("Lens alignment grid", prefs.lensAlign) { onChange(prefs.copy(lensAlign = it)) }
        ToggleRow("Use coarse location", prefs.useDeviceLocation) { checked ->
            if (checked) onRequestLocation() else onChange(prefs.copy(useDeviceLocation = false))
        }
        LocationFields(prefs, onChange)
        Button(onClick = onCalibrate, modifier = Modifier.fillMaxWidth()) {
            Text("Calibrate lenses on cover")
        }
        Button(onClick = onPreview, modifier = Modifier.fillMaxWidth()) {
            Text("Preview on this screen")
        }
        OutlinedButton(onClick = onShowCover, modifier = Modifier.fillMaxWidth()) {
            Text("Show on cover display now", color = palette.foreground)
        }
        OutlinedButton(onClick = onRearTransfer, modifier = Modifier.fillMaxWidth()) {
            Text("Move activity to rear display", color = palette.foreground)
        }
        OutlinedButton(onClick = onDreamSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Screensaver settings", color = palette.foreground)
        }
        OutlinedButton(onClick = onFullScreenIntent, modifier = Modifier.fillMaxWidth()) {
            Text("Full-screen intent access", color = palette.foreground)
        }
        OutlinedButton(onClick = onBatterySettings, modifier = Modifier.fillMaxWidth()) {
            Text("Ignore battery optimization", color = palette.foreground)
        }
        OutlinedButton(onClick = onNotificationSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Notification settings", color = palette.foreground)
        }
        Text(
            text = "Digits: Bitcount Grid Double. Text: JetBrains Mono Bold. Both SIL OFL.",
            style = terminalTextStyle(font, 14.sp, palette.darkForeground, FontWeight.Bold),
        )
        Text(
            text = "diagnostics",
            style = terminalTextStyle(font, 16.sp, palette.cyan, FontWeight.W500),
        )
        Text(
            text = diagnostics,
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.lighterBackground)
                .padding(12.dp),
            style = terminalTextStyle(font, 12.sp, palette.darkForeground),
        )
    }
}

@Composable
private fun LocationFields(prefs: ClockPreferences, onChange: (ClockPreferences) -> Unit) {
    val palette = LocalPalette.current
    val font = LocalMono.current
    var label by remember { mutableStateOf(prefs.locationLabel) }
    var lat by remember { mutableStateOf(prefs.latitude.toString()) }
    var lon by remember { mutableStateOf(prefs.longitude.toString()) }
    OutlinedTextField(
        value = label,
        onValueChange = {
            label = it
            onChange(prefs.copy(locationLabel = it.ifBlank { "New York" }))
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("City label", fontFamily = font) },
        textStyle = terminalTextStyle(font, 16.sp, palette.foreground, FontWeight.Bold),
        singleLine = true,
    )
    OutlinedTextField(
        value = lat,
        onValueChange = {
            lat = it
            it.toFloatOrNull()?.let { value -> onChange(prefs.copy(latitude = value)) }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Latitude", fontFamily = font) },
        textStyle = terminalTextStyle(font, 16.sp, palette.foreground, FontWeight.Bold),
        singleLine = true,
    )
    OutlinedTextField(
        value = lon,
        onValueChange = {
            lon = it
            it.toFloatOrNull()?.let { value -> onChange(prefs.copy(longitude = value)) }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Longitude", fontFamily = font) },
        textStyle = terminalTextStyle(font, 16.sp, palette.foreground, FontWeight.Bold),
        singleLine = true,
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val font = LocalMono.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            style = terminalTextStyle(font, 16.sp, palette.foreground),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedTrackColor = palette.accent,
                checkedThumbColor = palette.brightForeground,
                uncheckedTrackColor = palette.lighterBackground,
                uncheckedThumbColor = palette.darkForeground,
                uncheckedBorderColor = palette.muted,
            ),
        )
    }
}
