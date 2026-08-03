package com.example.divvyup.integration.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen

/**
 * FAB extendido (pill) — fuente única para todos los FABs "Nuevo gasto",
 * "Crear grupo", "Exportar", etc.
 *
 * Sustituye el patrón `FloatingActionButton + Row(icon + label)` que se
 * repetía en GroupDetail y AnalyticsExportFab.
 */
@Composable
fun PillFab(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
        containerColor = JungleGreen,
        contentColor = Color.White,
        modifier = modifier.shadow(
            elevation = 12.dp,
            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
            ambientColor = JungleGreen.copy(alpha = 0.25f),
            spotColor = JungleGreen.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

/**
 * Variante simple: solo icono, sin label — para casos donde el FAB no
 * admite texto (mini-FAB en cards).
 */
@Composable
fun IconFab(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp)
        )
    }
}