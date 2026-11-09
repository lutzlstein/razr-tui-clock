package com.coryblische.razrclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.coryblische.razrclock.R

/** JetBrains Mono Bold, the owner's terminal face. Used for every label except the clock digits. */
@Composable
fun rememberJetBrainsMono(): FontFamily = remember {
    FontFamily(
        Font(R.font.jetbrains_mono_bold, weight = FontWeight.Bold),
    )
}
