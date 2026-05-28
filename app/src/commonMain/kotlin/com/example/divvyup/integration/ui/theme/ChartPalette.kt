package com.example.divvyup.integration.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

internal data class ChartPalette(
    val themeAligned: List<Color>,
    val extendedContrast: List<Color>
) {
    val all: List<Color> = themeAligned + extendedContrast
}

@Composable
internal fun rememberChartPalette(): List<Color> {
    val colorScheme = MaterialTheme.colorScheme
    val palette = remember(colorScheme) {
        buildAnalyticsChartPalette(colorScheme)
    }
    return palette.all
}

private fun buildAnalyticsChartPalette(colorScheme: ColorScheme): ChartPalette {
    val themeAligned = listOf(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary,
        colorScheme.error,
        JungleGreenLight,
        BarkBrownLight,
        JungleGreenMid,
        MossGold,
        AppRed,
        Soil
    )

    // Extra colors with strong hue separation. They are used only after theme-like tones.
    val extendedContrast = listOf(
        Color(0xFF1E88E5),
        Color(0xFFD81B60),
        Color(0xFF00897B),
        Color(0xFF3949AB),
        Color(0xFFF4511E),
        Color(0xFF8E24AA),
        Color(0xFF00ACC1),
        Color(0xFF43A047),
        Color(0xFF5E35B1),
        Color(0xFFEC407A),
        Color(0xFF546E7A),
        Color(0xFF9CCC65)
    )

    return ChartPalette(
        themeAligned = themeAligned,
        extendedContrast = extendedContrast
    )
}

