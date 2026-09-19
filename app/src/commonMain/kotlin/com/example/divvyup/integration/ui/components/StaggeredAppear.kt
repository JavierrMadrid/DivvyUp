package com.example.divvyup.integration.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.example.divvyup.integration.ui.theme.DivvyUpMotion

/**
 * Entrada escalonada para ítems de listas.
 *
 * Anima alpha + desplazamiento vertical con un pequeño retardo creciente por
 * índice (máx. 8 pasos) para dar sensación de fluidez al aparecer la lista.
 */
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index.coerceAtMost(8) * 45).toLong())
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(DivvyUpMotion.Long, easing = DivvyUpMotion.EmphasizedDecelerate),
        label = "stagger-progress"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * 28f
        }
    ) {
        content()
    }
}
