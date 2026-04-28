package com.example.divvyup.integration.ui.screens.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen

/**
 * FAB de exportación con menú expandible (PDF, Excel, CSV, Texto).
 * Se coloca dentro de un `Box` con `Alignment.BottomEnd`.
 * [expanded] y [onExpandedChange] permiten al padre controlar el estado (p.ej. para dibujar un scrim).
 */
@Composable
internal fun AnalyticsExportFab(
    filtered: List<Spend>,
    periodLabel: String,
    onExportText: (List<Spend>) -> Unit,
    onExportCsv: (List<Spend>) -> Unit,
    onExportPdf: (List<Spend>, String) -> Unit,
    onExportExcel: (List<Spend>, String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(end = 20.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (expanded) {
            ExportActionRow(
                label = "PDF",
                icon = Icons.Default.PictureAsPdf,
                contentDescription = "Exportar PDF",
                onClick = { onExportPdf(filtered, periodLabel); onExpandedChange(false) }
            )
            ExportActionRow(
                label = "Excel",
                icon = Icons.Default.TableChart,
                contentDescription = "Exportar Excel",
                onClick = { onExportExcel(filtered, periodLabel); onExpandedChange(false) }
            )
            ExportActionRow(
                label = "CSV",
                icon = Icons.Default.TableChart,
                contentDescription = "Exportar CSV",
                onClick = { onExportCsv(filtered); onExpandedChange(false) }
            )
            ExportActionRow(
                label = "Texto",
                icon = Icons.Default.Description,
                contentDescription = "Exportar texto",
                onClick = { onExportText(filtered); onExpandedChange(false) }
            )
        }

        FloatingActionButton(
            onClick = { onExpandedChange(!expanded) },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
            containerColor = JungleGreen,
            contentColor = Color.White,
            modifier = Modifier.shadow(
                12.dp,
                RoundedCornerShape(DivvyUpTokens.RadiusPill),
                ambientColor = JungleGreen.copy(alpha = 0.25f),
                spotColor = JungleGreen.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Share,
                    contentDescription = "Exportar",
                    modifier = Modifier.size(DivvyUpTokens.IconMd)
                )
                Text(text = "Exportar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// ExportActionRow (fila de etiqueta + mini-FAB)
// ---------------------------------------------------------------------------

@Composable
internal fun ExportActionRow(
    label: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = JungleGreen,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.shadow(
                6.dp, CircleShape,
                ambientColor = JungleGreen.copy(alpha = 0.3f),
                spotColor = JungleGreen.copy(alpha = 0.4f)
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(DivvyUpTokens.IconMd)
            )
        }
    }
}

