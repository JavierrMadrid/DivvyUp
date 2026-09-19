package com.example.divvyup.integration.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Shapes
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.SetNavigationBarAppearance

// ── Shapes "Soft Rounded" — todo más redondeado y amable ─────────────────────
private val DivvyUpShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small      = RoundedCornerShape(18.dp),
    medium     = RoundedCornerShape(22.dp),
    large      = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun DivvyUpTheme(
    themeMode: ThemeMode = ThemePreferenceHolder.themeMode.collectAsState().value,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
        ThemeMode.SYSTEM -> systemDark
    }
    val colorScheme = if (darkTheme) DarkColors else LightColors

    SetNavigationBarAppearance(useDarkIcons = !darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = divvyUpTypography(),
        shapes      = DivvyUpShapes,
        content     = content
    )
}

@Composable
fun appOutlinedTextFieldColors(): TextFieldColors {
    // Deriva del esquema resuelto (no del sistema) para respetar el override de tema.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val focused = if (isDark) DarkTextBeige100 else JungleGreen
    val unfocused = if (isDark) DarkTextBeige200 else MaterialTheme.colorScheme.outline
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = focused,
        unfocusedBorderColor = unfocused,
        focusedLabelColor = focused,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = focused,
        focusedLeadingIconColor = focused,
        focusedTrailingIconColor = focused,
        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledBorderColor = unfocused.copy(alpha = 0.5f),
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledContainerColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    )
}

