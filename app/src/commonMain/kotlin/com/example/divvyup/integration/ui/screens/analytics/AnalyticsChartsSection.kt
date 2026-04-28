package com.example.divvyup.integration.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.divvyup.integration.ui.screens.fmt2
import com.example.divvyup.integration.ui.screens.participantAvatarPalette
import com.example.divvyup.integration.ui.theme.*

// ---------------------------------------------------------------------------
// Palette helpers
// ---------------------------------------------------------------------------

private val chartPaletteTokens = listOf(
    JungleGreen, BarkBrown, MossGold, JungleGreenMid, Amber,
    BarkBrownLight, JungleGreenLight, Soil, BarkBrownDark, JungleGreenDark
)

@Composable
internal fun rememberChartPalette(): List<Color> {
    val isDark = MaterialTheme.colorScheme.surface == DarkJungleSurface ||
            MaterialTheme.colorScheme.background.red < 0.3f
    return if (isDark) listOf(
        JungleGreenLight, BarkBrownLight, Amber, JungleGreenMid, MossGold,
        JungleGreen, BarkBrown, JungleGreenDark, Soil, BarkBrownDark
    ) else chartPaletteTokens
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
                            drawArc(
                                color = chartPalette[index % chartPalette.size],
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(chartPalette[index % chartPalette.size])
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
                drawArc(
                    color = palette[index % palette.size],
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
// DonutChartFullscreenDialog
// ---------------------------------------------------------------------------

@Composable
internal fun DonutChartFullscreenDialog(
    entries: List<DonutEntry>,
    currency: String,
    onDismiss: () -> Unit
) {
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
                        text = "Detalle por categoría",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DonutChart(entries = entries)
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(entries, key = { it.label }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(text = entry.icon, fontSize = 18.sp)
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${entry.count} gasto${if (entry.count == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${entry.value.toDouble().fmt2()} $currency",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEachIndexed { index, entry ->
                    val fraction = (entry.value / maxValue).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = entry.value.toDouble().fmt2(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            chartPalette[index % chartPalette.size],
                                            chartPalette[index % chartPalette.size].copy(alpha = 0.7f)
                                        )
                                    )
                                )
                        )
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            entries.forEachIndexed { index, entry ->
                val fraction = (entry.value / maxValue).coerceIn(0f, 1f)
                val percentage = if (total > 0) (entry.value / total.toFloat()) * 100f else 0f
                val isMinBar = entries.size > 1 && entry.value == minValue
                val barColor = if (isMinBar) {
                    MaterialTheme.colorScheme.error
                } else {
                    chartPalette[index % chartPalette.size]
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
                            text = "${entry.value.toDouble().fmt2()} $currency",
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

