package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.ActivityEventType
import com.example.divvyup.domain.model.ActivityLog
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreen100
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun ActivityTab(
    activityLog: List<ActivityLog>,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activityLog.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("📋", fontSize = 48.sp)
                Text(
                    "Sin actividad registrada aún",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Los gastos, liquidaciones y cambios en el grupo aparecerán aquí.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Historial de actividad",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${activityLog.size} eventos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapSm)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(DivvyUpTokens.IconSm)
                )
                Text(
                    "Se muestran los eventos del último mes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
        items(activityLog, key = { it.id }) { entry ->
            ActivityLogItem(entry)
        }
    }
}

@Composable
private fun ActivityLogItem(entry: ActivityLog) {
    val (icon, iconBg) = entry.eventType.iconAndColor()
    val dateStr = entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).let { dt ->
        val d = dt.date
        val h = dt.hour.toString().padStart(2, '0')
        val m = dt.minute.toString().padStart(2, '0')
        "${d.day} ${MES_CORTO[d.month.number]} ${d.year} · $h:$m"
    }

    // Separar cabecera de líneas de cambio (generadas con "• campo: antes → después")
    val lines = entry.description.split("\n")
    val headline = lines.first()
    val changeLines = lines.drop(1).filter { it.isNotBlank() }

    Card(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = Color.Black.copy(0.05f), spotColor = Color.Black.copy(0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                val iconTint = when (entry.eventType) {
                    ActivityEventType.GASTO_ELIMINADO,
                    ActivityEventType.LIQUIDACION_ELIMINADA -> Color(0xFFB71C1C)
                    else -> JungleGreenDark
                }
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(DivvyUpTokens.IconMd))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    headline,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // Líneas de cambio: cada "• campo: antes → después"
                if (changeLines.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    changeLines.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
                if (!entry.actorName.isNullOrBlank()) {
                    Text(
                        "por ${entry.actorName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun ActivityEventType.iconAndColor(): Pair<ImageVector, Color> = when (this) {
    ActivityEventType.GASTO_CREADO           -> Icons.Default.AddCircle    to JungleGreen100
    ActivityEventType.GASTO_EDITADO          -> Icons.Default.Edit          to JungleGreen100
    ActivityEventType.GASTO_ELIMINADO        -> Icons.Default.Delete        to Color(0xFFFFE0E0)
    ActivityEventType.PARTICIPANTE_ANADIDO   -> Icons.Default.PersonAdd     to JungleGreen100
    ActivityEventType.LIQUIDACION_CREADA     -> Icons.Default.CheckCircle   to JungleGreen100
    ActivityEventType.LIQUIDACION_ELIMINADA  -> Icons.Default.Cancel        to Color(0xFFFFE0E0)
}

