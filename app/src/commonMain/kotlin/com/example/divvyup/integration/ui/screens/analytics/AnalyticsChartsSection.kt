package com.example.divvyup.integration.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.divvyup.integration.ui.screens.fmt2
import com.example.divvyup.integration.ui.screens.participantAvatarPalette
import com.example.divvyup.integration.ui.theme.*
import kotlin.math.abs
import kotlin.math.round

private fun DonutEntry.chartColorKey(): String = "$label|$icon"

private fun AnalyticsBreakdownEntry.chartColorKey(): String = "$label|$icon"

private fun Double.fmtBarAmount(): String {
    val rounded = round(this * 100.0) / 100.0
    return if (abs(rounded % 1.0) < 0.005) rounded.toLong().toString() else rounded.fmt2()
}


// ---------------------------------------------------------------------------
// DonutChartCard
// ---------------------------------------------------------------------------

@Composable
internal fun DonutChartCard(
    entries: List<DonutEntry>,
    currency: String,
    title: String,
    onFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val chartPalette = rememberChartPalette()
    val chartColorMap = rememberChartColorMap(entries.map { it.chartColorKey() })
    val total = entries.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)

    Card(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                3.dp,
                RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (onFullscreen != null) {
                    TextButton(onClick = onFullscreen) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null,
                            modifier = Modifier.size(DivvyUpTokens.IconSm)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Ampliar")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = size.width * 0.18f
                        val radius = (size.minDimension - strokeWidth) / 2f
                        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                        val arcSize = Size(radius * 2f, radius * 2f)
                        var startAngle = -90f
                        entries.forEachIndexed { index, entry ->
                            val sweepAngle = (entry.value / total) * 360f
                            val entryColor = chartColorMap[entry.chartColorKey()]
                                ?: chartPalette[index % chartPalette.size]
                            drawArc(
                                color = entryColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle - 2f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${entries.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "categorías",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    entries.take(5).forEachIndexed { index, entry ->
                        val percentage = (entry.value / total) * 100f
                        val entryColor = chartColorMap[entry.chartColorKey()]
                            ?: chartPalette[index % chartPalette.size]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(entryColor)
                            )
                            Text(
                                text = entry.icon,
                                fontSize = 13.sp,
                                modifier = Modifier.width(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${entry.value.toDouble().fmt2()} $currency · ${percentage.toDouble().fmt2()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (entries.size > 5) {
                        Text(
                            text = "+${entries.size - 5} más",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// DonutChart (standalone, usado en el fullscreen dialog)
// ---------------------------------------------------------------------------

@Composable
internal fun DonutChart(
    entries: List<DonutEntry>,
    modifier: Modifier = Modifier,
    chartSize: Dp = 220.dp
) {
    val palette = rememberChartPalette()
    val chartColorMap = rememberChartColorMap(entries.map { it.chartColorKey() })
    val total = entries.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)

    Box(modifier = modifier.size(chartSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.width * 0.16f
            val radius = (size.minDimension - strokeWidth) / 2f
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(radius * 2f, radius * 2f)
            var startAngle = -90f
            entries.forEachIndexed { index, entry ->
                val sweepAngle = (entry.value / total) * 360f
                val entryColor = chartColorMap[entry.chartColorKey()]
                    ?: palette[index % palette.size]
                drawArc(
                    color = entryColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 2f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = entries.sumOf { it.count }.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "gastos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// AnalyticsCardFullscreenDialog
// ---------------------------------------------------------------------------

@Composable
internal fun AnalyticsCardFullscreenDialog(
    cardTitle: String,
    tablePrimaryHeader: String,
    breakdownEntries: List<AnalyticsBreakdownEntry>,
    barEntries: List<AnalyticsBreakdownEntry> = breakdownEntries,
    showCategoryIconLabelsInBars: Boolean = false,
    currency: String,
    initialTab: AnalyticsExpandedTab,
    onDismiss: () -> Unit
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = cardTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                // Tabs integrados sin fondo destacado — se funden con la card
                Surface(
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AnalyticsExpandedTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            Surface(
                                onClick = { selectedTab = tab },
                                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                color = if (isSelected) MaterialTheme.colorScheme.surface
                                        else Color.Transparent,
                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
                when (selectedTab) {
                    AnalyticsExpandedTab.ROSQUILLA -> {
                        if (breakdownEntries.isNotEmpty()) {
                            AnalyticsDonutTabContent(
                                breakdownEntries = breakdownEntries,
                                tablePrimaryHeader = tablePrimaryHeader,
                                currency = currency
                            )
                        } else {
                            EmptyFullscreenState("No hay datos para mostrar en este gráfico")
                        }
                    }

                    AnalyticsExpandedTab.BARRAS -> {
                        if (breakdownEntries.isNotEmpty()) {
                            AnalyticsBarsTabContent(
                                breakdownEntries = barEntries,
                                showCategoryIconLabels = showCategoryIconLabelsInBars,
                                tablePrimaryHeader = tablePrimaryHeader,
                                currency = currency
                            )
                        } else {
                            EmptyFullscreenState("No hay datos para mostrar en este gráfico")
                        }
                    }

                    AnalyticsExpandedTab.RANKING -> {
                        if (breakdownEntries.isNotEmpty()) {
                            AnalyticsRankingTabContent(
                                breakdownEntries = breakdownEntries,
                                tablePrimaryHeader = tablePrimaryHeader,
                                currency = currency
                            )
                        } else {
                            EmptyFullscreenState("No hay datos para mostrar en el ranking")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsDonutTabContent(
    breakdownEntries: List<AnalyticsBreakdownEntry>,
    tablePrimaryHeader: String,
    currency: String
) {
    val donutEntries = remember(breakdownEntries) {
        breakdownEntries.map {
            DonutEntry(
                label = it.label,
                icon = it.icon,
                value = it.total.toFloat(),
                count = it.spendCount,
                color = it.color
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                DonutChart(entries = donutEntries)
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            AnalyticsBreakdownTable(
                entries = breakdownEntries,
                primaryHeader = tablePrimaryHeader,
                currency = currency
            )
        }
    }
}

@Composable
private fun AnalyticsBarsTabContent(
    breakdownEntries: List<AnalyticsBreakdownEntry>,
    showCategoryIconLabels: Boolean,
    tablePrimaryHeader: String,
    currency: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            AnalyticsVerticalBarsPreview(
                entries = breakdownEntries,
                currency = currency,
                showCategoryIconLabels = showCategoryIconLabels
            )
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            AnalyticsBreakdownTable(
                entries = breakdownEntries,
                primaryHeader = tablePrimaryHeader,
                currency = currency
            )
        }
    }
}

@Composable
private fun AnalyticsRankingTabContent(
    breakdownEntries: List<AnalyticsBreakdownEntry>,
    tablePrimaryHeader: String,
    currency: String
) {
    val sorted = remember(breakdownEntries) { breakdownEntries.sortedByDescending { it.total } }
    val maxTotal = sorted.firstOrNull()?.total?.coerceAtLeast(0.001) ?: 0.001
    val totalAll = sorted.sumOf { it.total }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = tablePrimaryHeader,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Cantidad",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        itemsIndexed(sorted) { index, entry ->
            val fraction = (entry.total / maxTotal).toFloat().coerceIn(0f, 1f)
            val percentage = if (totalAll > 0.0) (entry.total / totalAll) * 100.0 else 0.0
            val medalColor = when (index) {
                0 -> MedalGold   // oro
                1 -> MedalSilver // plata
                2 -> MedalBronze // bronce
                else -> JungleGreen
            }
            val medalTextColor = when (index) {
                0 -> MedalGoldText
                1 -> MedalSilverText
                2 -> MedalBronzeText
                else -> Color.White
            }
            val barColor = medalColor
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(medalColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = medalTextColor
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = entry.icon, fontSize = 14.sp)
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${entry.spendCount} gasto${if (entry.spendCount == 1) "" else "s"} · ${percentage.fmt2()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${entry.total.fmt2()} $currency",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsBreakdownTable(
    entries: List<AnalyticsBreakdownEntry>,
    primaryHeader: String,
    currency: String
) {
    val total = entries.sumOf { it.total }
    val palette = rememberChartPalette()
    val chartColorMap = rememberChartColorMap(entries.map { it.chartColorKey() })

    // Anchos fijos para columnas numéricas — alineación cuadriculada
    val colGastos = 48.dp
    val colCantidad = 72.dp
    val colPct = 52.dp

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        // Cabecera
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = primaryHeader,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Gastos",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(colGastos),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = currency,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(colCantidad),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(colPct),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        entries.forEachIndexed { index, entry ->
            val percentage = if (total > 0.0) (entry.total / total) * 100.0 else 0.0
            val legendColor = chartColorMap[entry.chartColorKey()]
                ?: palette[index % palette.size]
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(legendColor)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${entry.icon} ${entry.label}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.spendCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(colGastos),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = entry.total.fmt2(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(colCantidad),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${percentage.fmt2()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(colPct),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        }
    }
}

@Composable
private fun AnalyticsVerticalBarsPreview(
    entries: List<AnalyticsBreakdownEntry>,
    currency: String,
    showCategoryIconLabels: Boolean
) {
    val palette = rememberChartPalette()
    val chartColorMap = rememberChartColorMap(entries.map { it.chartColorKey() })
    val maxValue = entries.maxOfOrNull { it.total.toFloat() }?.coerceAtLeast(1f) ?: 1f

    Column(modifier = Modifier.fillMaxWidth()) {
        // Área de barras con altura fija
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            entries.take(12).forEachIndexed { index, entry ->
                val fraction = (entry.total.toFloat() / maxValue).coerceIn(0.02f, 1f)
                val barColor = chartColorMap[entry.chartColorKey()]
                    ?: palette[index % palette.size]
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = entry.total.fmtBarAmount(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        barColor,
                                        barColor.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )
                }
            }
        }
        // Etiquetas de mes siempre visibles bajo las barras
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            entries.take(12).forEach { entry ->
                if (showCategoryIconLabels) {
                    Text(
                        text = entry.icon,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Text(
            text = "Importes en $currency",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}


@Composable
private fun EmptyFullscreenState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// MonthlyBarChartCard
// ---------------------------------------------------------------------------

@Composable
internal fun MonthlyBarChartCard(
    entries: List<BarEntry>,
    currency: String,
    title: String,
    onFullscreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val chartPalette = rememberChartPalette()
    val maxValue = entries.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f

    Card(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                3.dp,
                RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (onFullscreen != null) {
                    TextButton(onClick = onFullscreen) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null,
                            modifier = Modifier.size(DivvyUpTokens.IconSm)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Ampliar")
                    }
                }
            }
            // Área de barras
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEachIndexed { index, entry ->
                    val fraction = (entry.value / maxValue).coerceIn(0.02f, 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = entry.value.toDouble().fmtBarAmount(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            chartPalette[index % chartPalette.size],
                                            chartPalette[index % chartPalette.size].copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
            // Etiquetas de mes
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entries.forEach { entry ->
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                text = "Importes en $currency",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// HorizontalBarChartCard
// ---------------------------------------------------------------------------

@Composable
internal fun HorizontalBarChartCard(
    entries: List<BarEntry>,
    currency: String,
    total: Double,
    title: String,
    onFullscreen: (() -> Unit)? = null,
    payerBreakdown: List<AnalyticsBreakdownEntry> = emptyList(),
    balanceMap: Map<Long, com.example.divvyup.domain.model.ParticipantBalance> = emptyMap(),
    participantMap: Map<Long, com.example.divvyup.domain.model.Participant> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val chartPalette = rememberChartPalette()
    val maxValue = entries.maxOfOrNull { it.value } ?: 1f
    val minValue = entries.minOfOrNull { it.value } ?: 0f

    Card(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                3.dp,
                RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (onFullscreen != null) {
                    TextButton(onClick = onFullscreen) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null,
                            modifier = Modifier.size(DivvyUpTokens.IconSm)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Ampliar")
                    }
                }
            }
            entries.forEachIndexed { index, entry ->
                val fraction = (entry.value / maxValue).coerceIn(0f, 1f)
                val percentage = if (total > 0) (entry.value / total.toFloat()) * 100f else 0f
                val isMinBar = entries.size > 1 && entry.value == minValue
                
                // Encontrar el participante correspondiente y su balance
                val correspondingPayer = payerBreakdown.find { it.label == entry.label }
                val payerId = participantMap.entries.find { it.value.name == entry.label }?.key
                val balance = payerId?.let { balanceMap[it] }
                
                // Determinar color basado en el balance del participante
                val barColor = when {
                    // Si hay información de balance, usarla
                    balance != null -> when {
                        balance.netBalance > 0.005 -> SuccessGreen               // Verde — le deben dinero
                        balance.netBalance < -0.005 -> ErrorRed                   // Rojo — debe dinero
                        else -> WarningAmber                                     // Amarillo/naranja — equilibrio
                    }
                    // Si no hay balance, usar el color antiguo (mínimo = rojo, resto = paleta)
                    isMinBar -> MaterialTheme.colorScheme.error
                    else -> chartPalette[index % chartPalette.size]
                }
                val avatarColor = participantAvatarPalette[entry.label.length % participantAvatarPalette.size]

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entry.label.first().uppercaseChar().toString(),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${entry.value.toDouble().fmtBarAmount()} $currency",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                        Text(
                            text = "${percentage.toDouble().fmt2()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(barColor, barColor.copy(alpha = 0.7f))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

