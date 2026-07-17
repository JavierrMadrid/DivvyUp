package com.example.divvyup.integration.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.integration.ui.theme.Amber
import com.example.divvyup.integration.ui.theme.BarkBrown
import com.example.divvyup.integration.ui.theme.BarkBrownDark
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.theme.JungleGreenMid
import com.example.divvyup.integration.ui.theme.MossGold
import com.example.divvyup.integration.ui.theme.Soil

/**
 * Paleta unificada de avatares — 8 colores jungle-aligned para iniciales.
 */
val participantAvatarPalette = listOf(
    JungleGreen, JungleGreenDark, BarkBrown,
    MossGold, Soil, JungleGreenMid,
    BarkBrownDark, Amber
)

/**
 * Avatar circular con iniciales — fuente única para todas las pantallas.
 *
 * @param initials 1–3 letras a mostrar.
 * @param paletteIndex índice en la paleta jungle (se aplica módulo para estabilidad).
 * @param ring dibuja un anillo claro alrededor del avatar (útil sobre imágenes).
 * @param onClick callback opcional para hacerlo tappable.
 */
@Composable
fun AppAvatar(
    initials: String,
    paletteIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    ring: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val palette = participantAvatarPalette
    val bg = palette[((paletteIndex % palette.size) + palette.size) % palette.size]
    val sizeStyle = when {
        size >= 48.dp -> 18.sp
        size >= 36.dp -> 16.sp
        else -> 13.sp
    }
    val baseModifier = modifier
        .size(size)
        .shadow(if (ring) 2.dp else 0.dp, CircleShape)
        .clip(CircleShape)
        .background(bg)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.uppercase().take(3),
            color = Color.White,
            fontSize = sizeStyle,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}