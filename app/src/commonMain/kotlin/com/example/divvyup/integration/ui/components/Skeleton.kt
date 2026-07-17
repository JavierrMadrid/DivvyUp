package com.example.divvyup.integration.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.theme.DivvyUpTokens

/**
 * Skeleton box — placeholder de carga con shimmer animado.
 *
 * API simple para reemplazar spinners y LinearProgressIndicators.
 *
 * ```kotlin
 * SkeletonBox(modifier = Modifier.fillMaxWidth().height(56.dp))
 * SkeletonBox(modifier = Modifier.size(80.dp), shape = CircleShape)
 * ```
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(DivvyUpTokens.RadiusRow)
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "skeleton-progress"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to base,
                        0.5f to highlight,
                        1f to base
                    ),
                    start = Offset(progress * 600f - 200f, 0f),
                    end = Offset(progress * 600f, 0f)
                )
            )
    )
}

/**
 * Skeleton card — slot visual de alto fijo para reemplazar cards durante carga.
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 80.dp
) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard)
    )
}