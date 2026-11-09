package com.coryblische.razrclock.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.coryblische.razrclock.R

/**
 * Bitcount Grid Double is a variable pixel face (axes wght, ELXP, ELSH, slnt, CRSV).
 * Weight stays near the regular master so the double stroke does not bloom into a
 * blob on OLED. A little element expansion (ELXP) keeps the stems visible at cover size.
 * Square elements (ELSH 0), upright (slnt 0), and roman rather than cursive (CRSV 0).
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun rememberBitcount(): FontFamily = remember {
    FontFamily(
        Font(
            R.font.bitcount_grid_double,
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("ELXP", 8f),
                FontVariation.Setting("ELSH", 0f),
                FontVariation.Setting("slnt", 0f),
                FontVariation.Setting("CRSV", 0f),
            ),
        ),
    )
}
