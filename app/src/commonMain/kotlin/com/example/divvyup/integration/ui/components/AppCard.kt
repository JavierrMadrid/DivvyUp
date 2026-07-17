package com.example.divvyup.integration.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.theme.DivvyUpTokens

/**
 * Tarjeta base de DivvyUp — premium fintech look.
 *
 * - Reemplaza el patrón `Card(elevation = 0) + Modifier.shadow(...)` con una única
 *   fuente de sombra y shape.
 * - `tonalElevation` controla el nivel de jerarquía visual:
 *   - `Flat` (default): sobre `surface`, sin sombra pronunciada.
 *   - `Elevated`: sobre `surfaceContainerLow`, con `shadowElevation` ligero.
 * - Para agregar tinte o borde, usar [containerColor]/[border].
 */
enum class AppCardLevel { Flat, Elevated }

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    level: AppCardLevel = AppCardLevel.Flat,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = null,
    containerColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(DivvyUpTokens.GapMdPlus),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(DivvyUpTokens.RadiusCard)
    val baseContainer = when (level) {
        AppCardLevel.Flat -> MaterialTheme.colorScheme.surface
        AppCardLevel.Elevated -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    // Press-scale micro-interaction — ≤ 2 % scale, ≤ 200 ms (gesture feedback).
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 800f),
        label = "card-press-scale"
    )
    val pressModifier = if (onClick != null) Modifier.scale(scale) else Modifier

    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor ?: baseContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (level == AppCardLevel.Elevated) 2.dp else 0.dp,
            pressedElevation = if (onClick != null) 1.dp else 0.dp
        ),
        border = border,
        interactionSource = interactionSource,
        modifier = modifier.then(pressModifier)
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}