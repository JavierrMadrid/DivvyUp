package com.example.divvyup.integration.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.integration.ui.theme.DivvyUpTokens

/**
 * Tabs pill unificados para barras superiores (GroupDetail, GroupSettings, etc.).
 *
 * Pensados para vivir BAJO un [AppTopBar] con `variant = Gradient`, que pinta
 * un gradiente verde oscuro. Por defecto la fila de tabs también pinta ese
 * mismo fondo (`containerColor` jungle oscuro) para que el bloque entero
 * (título + tabs) se vea como una cabecera continua con buen contraste en
 * tema claro. Si se quiere integrar con otro fondo, pasar otro `containerColor`.
 *
 * - Indicador animado (color de fondo del tab seleccionado).
 * - Tabs equi-distribuidos vía `Modifier.weight(1f)`.
 * - El caller controla el contenido (texto) y el callback de selección.
 */
@Composable
fun AppTabsRow(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabLabels: List<String>,
    containerColor: Color = Color(0xFF1B4332) // JungleGreenDark — encaja con TopBarVariant.Gradient
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = DivvyUpTokens.GapMd, vertical = DivvyUpTokens.GapSm),
        horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapXs)
    ) {
        tabLabels.forEachIndexed { index, label ->
            AppTab(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent,
        label = "tab-bg"
    ).value
    val contentColor = animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
        label = "tab-content"
    ).value
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = DivvyUpTokens.GapSm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Variante light/flat — para tabs dentro de barras claras (no gradient header).
 */
@Composable
fun AppTabsRowFlat(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabLabels: List<String>
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DivvyUpTokens.GapMd, vertical = DivvyUpTokens.GapSm),
        horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapXs)
    ) {
        tabLabels.forEachIndexed { index, label ->
            val containerColor = animateColorAsState(
                targetValue = if (index == selectedIndex)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                label = "flat-tab-bg"
            ).value
            val contentColor = animateColorAsState(
                targetValue = if (index == selectedIndex)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                label = "flat-tab-content"
            ).value
            Surface(
                onClick = { onSelect(index) },
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DivvyUpTokens.GapSm),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabLabels[index],
                        fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}