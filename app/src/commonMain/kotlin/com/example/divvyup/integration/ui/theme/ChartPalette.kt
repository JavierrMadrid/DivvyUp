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

@Composable
internal fun rememberChartColorMap(keys: List<String>): Map<String, Color> {
    val palette = rememberChartPalette()
    return remember(keys, palette) {
        keys
            .distinct()
            .mapIndexed { index, key -> key to palette[index % palette.size] }
            .toMap()
    }
}

private fun buildAnalyticsChartPalette(colorScheme: ColorScheme): ChartPalette {
    // Colores primero cercanos al tema, pero con separación visual clara.
    val themeAligned = listOf(
        colorScheme.primary,
        colorScheme.secondary,
        colorScheme.tertiary,
        colorScheme.error,
        Color(0xFF2F7F73),
        Color(0xFF516B8A),
        Color(0xFF8A5A7A),
        Color(0xFFB36A45),
        Color(0xFF7A8F3D),
        Color(0xFF6E5B95)
    )

    // Extensión para muchas categorías: siguen siendo armoniosos, pero menos ligados al tema.
    val extendedContrast = listOf(
        Color(0xFF1E88E5),
        Color(0xFFD95F5F),
        Color(0xFF00AFA3),
        Color(0xFF8E44AD),
        Color(0xFFEF6C00),
        Color(0xFF3949AB),
        Color(0xFFC2185B),
        Color(0xFF00838F),
        Color(0xFF7CB342),
        Color(0xFF5D4037),
        Color(0xFFF9A825),
        Color(0xFF6D4C41)
    )

    return ChartPalette(
        themeAligned = themeAligned,
        extendedContrast = extendedContrast
    )
}

