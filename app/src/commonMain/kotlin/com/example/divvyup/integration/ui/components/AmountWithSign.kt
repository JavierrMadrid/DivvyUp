package com.example.divvyup.integration.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cantidad numérica con **signo explícito** y **icono direccional** —
 * accesibilidad para daltónicos.
 *
 * - `+` y `-` siempre visibles en el texto formateado (delegado a
 *   [AmountText] que ya incluye `sign` en `formatAmount`).
 * - Icono `TrendingUp` / `TrendingDown` / `TrendingFlat` refuerza el signo
 *   incluso si el color no se percibe.
 * - El color sigue disponible como accent secundario, pero el signo visible
 *   es el carrier principal de información.
 */
@Composable
fun AmountWithSign(
    amount: Double,
    modifier: Modifier = Modifier,
    currency: String? = null,
    positiveColor: Color = MaterialTheme.colorScheme.primary,
    negativeColor: Color = MaterialTheme.colorScheme.error,
    neutralColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight = FontWeight.Bold,
    showIcon: Boolean = true,
    iconSize: Dp = 16.dp
) {
    val isPositive = amount > 0.0
    val isNegative = amount < 0.0
    val color = when {
        isPositive -> positiveColor
        isNegative -> negativeColor
        else       -> neutralColor
    }
    val icon: ImageVector? = when {
        isPositive -> Icons.AutoMirrored.Filled.TrendingUp
        isNegative -> Icons.AutoMirrored.Filled.TrendingDown
        showIcon   -> Icons.AutoMirrored.Filled.TrendingFlat
        else       -> null
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null && showIcon) {
            Icon(
                imageVector = icon,
                contentDescription = null, // decorativo: el carrier principal es el texto +/-
                tint = color,
                modifier = Modifier.size(iconSize)
            )
        }
        AmountText(
            amount = amount,
            currency = currency,
            color = color,
            style = style,
            fontWeight = fontWeight
        )
    }
}
