package com.example.divvyup.integration.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.theme.JungleGreen

data class AppFilterChipPalette(
    val selectedColor: Color,
    val unselectedColor: Color,
    val unselectedTextColor: Color,
    val summaryContainerColor: Color
)

@Composable
fun rememberAppFilterChipPalette(
    selectedColor: Color = JungleGreen
): AppFilterChipPalette {
    val isDark = isSystemInDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    return remember(isDark, colorScheme, selectedColor) {
        AppFilterChipPalette(
            selectedColor = selectedColor,
            unselectedColor = if (isDark) colorScheme.surfaceContainerHigh else colorScheme.surfaceVariant,
            unselectedTextColor = colorScheme.onSurface,
            summaryContainerColor = if (isDark) colorScheme.surfaceContainerHighest else colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AppFilterLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun <T> AppFilterChipRow(
    items: List<T>,
    leadingAllLabel: String? = null,
    onLeadingAllClick: (() -> Unit)? = null,
    isLeadingAllSelected: Boolean = false,
    selectedColor: Color = JungleGreen,
    chipContent: @Composable (T) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (leadingAllLabel != null) {
            item {
                AppFilterChip(
                    label = leadingAllLabel,
                    selected = isLeadingAllSelected,
                    selectedColor = selectedColor,
                    onClick = { onLeadingAllClick?.invoke() }
                )
            }
        }
        items(items) { chipContent(it) }
    }
}


