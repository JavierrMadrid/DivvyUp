package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.integration.ui.components.AppFilterChip
import com.example.divvyup.integration.ui.components.AppSearchField
import com.example.divvyup.integration.ui.components.rememberAppFilterChipPalette
import com.example.divvyup.integration.ui.screens.analytics.*
import com.example.divvyup.integration.ui.theme.*
import com.example.divvyup.integration.ui.viewmodel.AnalyticsPeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock.System

private const val DEFAULT_UNCATEGORIZED_ICON = "📦"
private const val BUDGET_WARNING_COLOR_HEX = 0xFFF59E0B
private const val WARNING_TEXT_COLOR_HEX = 0xFF7C5200
private const val WARNING_CONTAINER_COLOR_HEX = 0xFFFFF3CD

// --- Tab: Analíticas ----------------------------------------------------------

@Composable
internal fun AnalyticsTab(
    spends: List<Spend>,
    categories: List<Category>,
    participants: List<Participant>,
    settlements: List<Settlement>,
    /** spendId → lista de shares para calcular el pago neto real por pagador. */
    spendSharesBySpend: Map<Long, List<SpendShare>> = emptyMap(),
    currency: String,
    searchQuery: String,
    selectedCategories: Set<Long>,
    selectedParticipants: Set<Long>,
    period: AnalyticsPeriod,
    onSearchQueryChange: (String) -> Unit,
    onCategoryToggle: (Long) -> Unit,
    onParticipantToggle: (Long) -> Unit,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    onClearFilters: () -> Unit,
    onExportText: (filteredSpends: List<Spend>) -> Unit = {},
    onExportCsv: (filteredSpends: List<Spend>) -> Unit = {},
    onExportPdf: (filteredSpends: List<Spend>, periodLabel: String) -> Unit = { _, _ -> },
    onExportExcel: (filteredSpends: List<Spend>, periodLabel: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val chipPalette = rememberAppFilterChipPalette(selectedColor = JungleGreen)
    val categoryMap by remember(categories) { derivedStateOf { categories.associateBy { it.id } } }
    val participantMap by remember(participants) { derivedStateOf { participants.associateBy { it.id } } }
    val effectiveSelectedCategories by remember(selectedCategories, categoryMap) {
        derivedStateOf { selectedCategories.intersect(categoryMap.keys) }
    }
    val now = remember { System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }

    val hasActiveFilters by remember(searchQuery, selectedCategories, selectedParticipants, period) {
        derivedStateOf {
            searchQuery.isNotBlank() ||
                    selectedCategories.isNotEmpty() ||
                    selectedParticipants.isNotEmpty() ||
                    period !is AnalyticsPeriod.Todo
        }
    }

    val periodFiltered by remember(spends, period) {
        derivedStateOf {
            spends.filter { spend ->
                val ldt = spend.date.toLocalDateTime(TimeZone.currentSystemDefault())
                when (period) {
                    is AnalyticsPeriod.Todo -> true
                    is AnalyticsPeriod.PorMes -> ldt.year == period.year && ldt.month == period.month
                    is AnalyticsPeriod.PorAnyo -> ldt.year == period.year
                    is AnalyticsPeriod.PorRango -> ldt.date >= period.desde && ldt.date <= period.hasta
                }
            }
        }
    }

    val filtered by remember(periodFiltered, effectiveSelectedCategories, selectedParticipants, searchQuery) {
        derivedStateOf {
            periodFiltered.filter { spend ->
                val matchesQuery = searchQuery.isBlank() || spend.concept.contains(searchQuery, ignoreCase = true)
                val matchesCategory = effectiveSelectedCategories.isEmpty() ||
                        (spend.categoryId != null && spend.categoryId in effectiveSelectedCategories)
                val matchesParticipant = selectedParticipants.isEmpty() || spend.payerId in selectedParticipants
                matchesQuery && matchesCategory && matchesParticipant
            }
        }
    }

    val totalFiltered by remember(filtered) { derivedStateOf { filtered.sumOf { it.amount } } }
    val nonEqualSpendCount by remember(filtered) {
        derivedStateOf { filtered.count { it.splitType != SplitType.EQUAL } }
    }

    // A2 — Estadísticas ampliadas: mediana, máximo
    val medianAmount by remember(filtered) {
        derivedStateOf {
            if (filtered.isEmpty()) 0.0
            else {
                val sorted = filtered.map { it.amount }.sorted()
                val mid = sorted.size / 2
                if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2.0
                else sorted[mid]
            }
        }
    }
    val maxSpend by remember(filtered) {
        derivedStateOf { filtered.maxByOrNull { it.amount } }
    }

    // Gastos reales (sin liquidaciones) para gráficos de pagador
    val settlementCategoryIds by remember(categories) {
        derivedStateOf {
            categories
                .filter { it.name.equals(SETTLEMENT_CATEGORY_NAME, ignoreCase = true) }
                .map { it.id }
                .toSet()
        }
    }

    val filteredNoSettlements by remember(filtered, settlementCategoryIds) {
        derivedStateOf {
            filtered.filterNot { spend ->
                spend.notes.trimStart().startsWith("__settlement_id:") ||
                    (spend.categoryId != null && spend.categoryId in settlementCategoryIds)
            }
        }
    }

    val byCategory by remember(filtered, categoryMap) {
        derivedStateOf {
            filtered
                .groupBy { it.categoryId }
                .map { (categoryId, spendsForCategory) ->
                    val category = categoryId?.let { categoryMap[it] }
                    CategoryBucket(
                        id = categoryId,
                        icon = category?.icon ?: DEFAULT_UNCATEGORIZED_ICON,
                        name = category?.name ?: "Sin categoría",
                        total = spendsForCategory.sumOf { it.amount },
                        count = spendsForCategory.size
                    )
                }
                .sortedByDescending { it.total }
        }
    }

    val budgetProgress by remember(byCategory, categoryMap) {
        derivedStateOf {
            byCategory.mapNotNull { bucket ->
                val category = bucket.id?.let { categoryMap[it] } ?: return@mapNotNull null
                val budget = category.budget ?: return@mapNotNull null
                Triple(category, bucket.total, budget)
            }
        }
    }

    val monthlyBreakdown by remember(filtered) {
        derivedStateOf {
            filtered
                .groupBy { spend ->
                    val ldt = spend.date.toLocalDateTime(TimeZone.currentSystemDefault())
                    ldt.year * 100 + ldt.month.number
                }
                .entries.sortedBy { it.key }.takeLast(12)
                .map { (key, monthlySpends) ->
                    val monthNumber = key % 100
                    val year = key / 100
                    AnalyticsBreakdownEntry(
                        label = "${MES_CORTO[monthNumber]}/${year.toString().takeLast(2)}",
                        icon = "📅",
                        total = monthlySpends.sumOf { it.amount },
                        spendCount = monthlySpends.size
                    )
                }
        }
    }

    val monthlyEntries by remember(monthlyBreakdown) {
        derivedStateOf {
            // Mantener orden cronológico (mes ascendente) para barras mensuales.
            monthlyBreakdown.map { BarEntry(label = it.label, value = it.total.toFloat()) }
        }
    }

    // A1 — Tendencia mes a mes: comparar último mes vs penúltimo
    val monthTrend by remember(monthlyBreakdown) {
        derivedStateOf {
            if (monthlyBreakdown.size >= 2) {
                val last = monthlyBreakdown.last().total
                val prev = monthlyBreakdown[monthlyBreakdown.size - 2].total
                if (prev > 0.0) ((last - prev) / prev) * 100.0 else null
            } else null
        }
    }

    val categoryBreakdown by remember(byCategory) {
        derivedStateOf {
            byCategory.map { bucket ->
                AnalyticsBreakdownEntry(
                    label = bucket.name,
                    icon = bucket.icon,
                    total = bucket.total,
                    spendCount = bucket.count
                )
            }
        }
    }

    val categoryBreakdownForBars by remember(categoryBreakdown) {
        derivedStateOf {
            categoryBreakdown.sortedBy { it.label.lowercase() }
        }
    }

    val payerBreakdown by remember(filteredNoSettlements, participantMap, spendSharesBySpend) {
        derivedStateOf {
            filteredNoSettlements
                .groupBy { it.payerId }
                .map { (payerId, payerSpends) ->
                    // Pago neto = lo que desembolsó cada uno - su propia parte del gasto
                    // Así se refleja cuánto pagó realmente por los demás
                    val netPaid = payerSpends.sumOf { spend ->
                        val payerShare = spendSharesBySpend[spend.id]
                            ?.firstOrNull { it.participantId == payerId }
                            ?.amount ?: 0.0
                        spend.amount - payerShare
                    }
                    AnalyticsBreakdownEntry(
                        label = participantMap[payerId]?.name.orEmpty().ifBlank { "Desconocido" },
                        icon = "👤",
                        total = netPaid.coerceAtLeast(0.0),
                        spendCount = payerSpends.size
                    )
                }
                .sortedByDescending { it.total }
        }
    }

    val payerBreakdownForBars by remember(payerBreakdown) {
        derivedStateOf {
            payerBreakdown.sortedBy { it.label.lowercase() }
        }
    }

    // Total neto pagado por todos los pagadores (suma de pagos netos = base para porcentajes)
    val totalNetPaid by remember(payerBreakdown) {
        derivedStateOf { payerBreakdown.sumOf { it.total } }
    }

    val netSettlements by remember(settlements) {
        derivedStateOf {
            val pairNet = mutableMapOf<Pair<Long, Long>, Double>()
            for (settlement in settlements) {
                val from = settlement.fromParticipantId
                val to = settlement.toParticipantId
                if (from < to) pairNet[from to to] = (pairNet[from to to] ?: 0.0) + settlement.amount
                else pairNet[to to from] = (pairNet[to to from] ?: 0.0) - settlement.amount
            }
            pairNet.mapNotNull { (pair, net) ->
                when {
                    net > 0.005 -> Triple(pair.first, pair.second, net)
                    net < -0.005 -> Triple(pair.second, pair.first, -net)
                    else -> null
                }
            }.sortedByDescending { it.third }
        }
    }

    var expandedCard by remember { mutableStateOf<AnalyticsCardType?>(null) }
    var exportExpanded by remember { mutableStateOf(false) }

    val periodLabel by remember(period) {
        derivedStateOf {
            when (period) {
                is AnalyticsPeriod.Todo -> "Todos los periodos"
                is AnalyticsPeriod.PorMes -> "${MES_NOMBRES[period.month.number - 1]} ${period.year}"
                is AnalyticsPeriod.PorAnyo -> "Año ${period.year}"
                is AnalyticsPeriod.PorRango -> "${formatLocalDate(period.desde)} - ${formatLocalDate(period.hasta)}"
            }
        }
    }

    when (expandedCard) {
        AnalyticsCardType.MENSUAL -> AnalyticsCardFullscreenDialog(
            cardTitle = "Evolución mensual",
            tablePrimaryHeader = "Mes",
            breakdownEntries = monthlyBreakdown,
            barEntries = monthlyBreakdown,
            currency = currency,
            initialTab = AnalyticsExpandedTab.BARRAS,
            onDismiss = { expandedCard = null }
        )

        AnalyticsCardType.CATEGORIA -> AnalyticsCardFullscreenDialog(
            cardTitle = "Por categoría",
            tablePrimaryHeader = "Categoría",
            breakdownEntries = categoryBreakdown,
            barEntries = categoryBreakdownForBars,
            currency = currency,
            initialTab = AnalyticsExpandedTab.ROSQUILLA,
            onDismiss = { expandedCard = null }
        )

        AnalyticsCardType.PAGADOR -> AnalyticsCardFullscreenDialog(
            cardTitle = "Por pagador",
            tablePrimaryHeader = "Pagador",
            breakdownEntries = payerBreakdown,
            barEntries = payerBreakdownForBars,
            currency = currency,
            initialTab = AnalyticsExpandedTab.BARRAS,
            onDismiss = { expandedCard = null }
        )

        null -> Unit
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Buscador ──────────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppSearchField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = "Buscar concepto",
                        onClear = { onSearchQueryChange("") },
                        modifier = Modifier.weight(1f).heightIn(min = DivvyUpTokens.ControlHeight)
                    )
                    IconButton(
                        onClick = onClearFilters,
                        enabled = hasActiveFilters,
                        modifier = Modifier
                            .size(DivvyUpTokens.ControlHeight)
                            .clip(RoundedCornerShape(DivvyUpTokens.RadiusControl))
                            .background(
                                if (hasActiveFilters) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAltOff,
                            contentDescription = "Limpiar filtros",
                            tint = if (hasActiveFilters) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(DivvyUpTokens.IconSm)
                        )
                    }
                }
            }

            // ── Selector de período ───────────────────────────────────────────────
            item {
                PeriodFilterSelector(
                    period = period,
                    currentYear = now.year,
                    currentMonth = now.month,
                    onPeriodChange = onPeriodChange
                )
            }

            // ── Chips de categoría ────────────────────────────────────────────────
            if (categories.isNotEmpty()) {
                item {
                    Text("Categoría", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories, key = { it.id }) { category ->
                            AppFilterChip(
                                label = "${category.icon} ${category.name}",
                                selected = category.id in selectedCategories,
                                selectedColor = chipPalette.selectedColor,
                                unselectedColor = chipPalette.unselectedColor,
                                unselectedTextColor = chipPalette.unselectedTextColor,
                                height = DivvyUpTokens.ChipHeight,
                                onClick = { onCategoryToggle(category.id) }
                            )
                        }
                    }
                }
            }

            // ── Chips de personas ─────────────────────────────────────────────────
            if (participants.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(2.dp))
                    Text("Persona", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(participants, key = { it.id }) { participant ->
                            val isSelected = participant.id in selectedParticipants
                            val avatarColor = participantAvatarPalette[participant.name.length % participantAvatarPalette.size]
                            Surface(
                                onClick = { onParticipantToggle(participant.id) },
                                shape = RoundedCornerShape(50.dp),
                                color = if (isSelected) avatarColor else chipPalette.unselectedColor,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White.copy(alpha = 0.3f) else avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = participant.name.first().uppercaseChar().toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = participant.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) Color.White else chipPalette.unselectedTextColor,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Separador ─────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(Modifier.height(8.dp))
            }

            // ── Resumen total (A1 + A2) ───────────────────────────────────────────
            item {
                AnalyticsSummaryCard(
                    totalFiltered = totalFiltered,
                    spendCount = filtered.size,
                    currency = currency,
                    medianAmount = medianAmount,
                    maxSpend = maxSpend,
                    monthTrend = monthTrend
                )
            }

            // ── Gráfico mensual ───────────────────────────────────────────────────
            if (monthlyEntries.size >= 2) {
                item {
                    MonthlyBarChartCard(
                        entries = monthlyEntries,
                        currency = currency,
                        title = "Evolución mensual",
                        onFullscreen = { expandedCard = AnalyticsCardType.MENSUAL }
                    )
                }
            }

            // ── Gráfico por categoría ─────────────────────────────────────────────
            if (byCategory.isNotEmpty()) {
                item {
                    DonutChartCard(
                        entries = byCategory.map { DonutEntry(it.name, it.icon, it.total.toFloat(), it.count) },
                        currency = currency,
                        title = "Por categoría",
                        onFullscreen = { expandedCard = AnalyticsCardType.CATEGORIA }
                    )
                }
            }

            // ── Presupuesto por categoría ─────────────────────────────────────────
            if (budgetProgress.isNotEmpty()) {
                item { BudgetProgressCard(budgetProgress = budgetProgress, currency = currency) }
            }

            // ── Gráfico por pagador ───────────────────────────────────────────────
            if (payerBreakdown.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    if (nonEqualSpendCount > 0) NonEqualWarningRow(nonEqualSpendCount = nonEqualSpendCount)
                    Spacer(Modifier.height(6.dp))
                    HorizontalBarChartCard(
                        entries = payerBreakdownForBars.map { BarEntry(label = it.label, value = it.total.toFloat()) },
                        currency = currency,
                        total = totalNetPaid,
                        title = "Por pagador",
                        onFullscreen = { expandedCard = AnalyticsCardType.PAGADOR }
                    )
                }
            }

            // ── Resumen neto de liquidaciones ─────────────────────────────────────
            if (settlements.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Resumen de liquidaciones",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (netSettlements.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(text = "✅", fontSize = 22.sp)
                                Text(
                                    text = "Todas las cuentas están saldadas",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                } else {
                    items(netSettlements, key = { "net_${it.first}_${it.second}" }) { (fromId, toId, netAmount) ->
                        SettlementNetRow(
                            fromName = participantMap[fromId]?.name ?: "Desconocido",
                            toName = participantMap[toId]?.name ?: "Desconocido",
                            netAmount = netAmount,
                            currency = currency
                        )
                    }
                }
            }

            // ── Lista de gastos filtrados ─────────────────────────────────────────
            if (filtered.isNotEmpty()) {
                item {
                    AnalyticsSpendList(
                        filtered = filtered,
                        currency = currency,
                        categoryMap = categoryMap,
                        participantMap = participantMap,
                        searchQuery = searchQuery,
                        selectedCategories = selectedCategories,
                        selectedParticipants = selectedParticipants,
                        period = period
                    )
                }
            } else if (spends.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "🔍", fontSize = 36.sp)
                            Text(
                                text = "Sin resultados para los filtros aplicados",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ── Scrim cuando el menú de exportación está abierto ─────────────────
        if (exportExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable { exportExpanded = false }
            )
        }

        // ── FAB de exportación ────────────────────────────────────────────────
        AnalyticsExportFab(
            filtered = filtered,
            periodLabel = periodLabel,
            onExportText = onExportText,
            onExportCsv = onExportCsv,
            onExportPdf = onExportPdf,
            onExportExcel = onExportExcel,
            expanded = exportExpanded,
            onExpandedChange = { exportExpanded = it },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

// ---------------------------------------------------------------------------
// Componentes privados del orquestador
// ---------------------------------------------------------------------------

/**
 * A1 + A2 — Card verde de resumen con: total, nº gastos, promedio,
 * mediana, gasto máximo y tendencia vs. mes anterior.
 */
@Composable
private fun AnalyticsSummaryCard(
    totalFiltered: Double,
    spendCount: Int,
    currency: String,
    medianAmount: Double,
    maxSpend: Spend?,
    monthTrend: Double?,
    modifier: Modifier = Modifier
) {
    val avg = if (spendCount > 0) totalFiltered / spendCount else 0.0
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = JungleGreen),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Fila superior: total + tendencia
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total gastado", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        text = "${totalFiltered.fmt2()} $currency",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$spendCount gastos", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                    // A1 — Indicador de tendencia
                    if (monthTrend != null) {
                        val isUp = monthTrend >= 0
                        val trendText = "${if (isUp) "▲" else "▼"} ${kotlin.math.abs(monthTrend).fmt2().dropLastWhile { it == '0' }.trimEnd('.')}% vs. mes ant."
                        Surface(
                            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                            color = if (isUp) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = trendText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isUp) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                            )
                        }
                    }
                }
            }

            // A2 — Fila de estadísticas: promedio · mediana · máximo
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapMd)
            ) {
                StatMiniCard(label = "Promedio", value = "${avg.fmt2()} $currency", modifier = Modifier.weight(1f))
                StatMiniCard(label = "Mediana", value = "${medianAmount.fmt2()} $currency", modifier = Modifier.weight(1f))
                if (maxSpend != null) {
                    StatMiniCard(
                        label = "Mayor gasto",
                        value = "${maxSpend.amount.fmt2()} $currency",
                        subtitle = maxSpend.concept,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    label: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BudgetProgressCard(
    budgetProgress: List<Triple<Category, Double, Double>>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Spacer(Modifier.height(2.dp))
    Card(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Presupuesto mensual", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            budgetProgress.forEach { (category, spent, budget) ->
                val fraction = (spent / budget).coerceIn(0.0, 1.0).toFloat()
                val overBudget = spent > budget
                val progressColor = when {
                    overBudget -> MaterialTheme.colorScheme.error
                    fraction > 0.8f -> Color(BUDGET_WARNING_COLOR_HEX)
                    else -> JungleGreen
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = category.icon, fontSize = 16.sp, modifier = Modifier.width(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(text = category.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(text = "${spent.fmt2()} / ${budget.fmt2()} $currency", style = MaterialTheme.typography.labelSmall, color = progressColor, fontWeight = FontWeight.SemiBold)
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50.dp)),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    if (overBudget) {
                        Text(text = "⚠️ Superado en ${(spent - budget).fmt2()} $currency", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun NonEqualWarningRow(nonEqualSpendCount: Int, modifier: Modifier = Modifier) {
    var showWarningPopup by remember { mutableStateOf(false) }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(onClick = { showWarningPopup = true }, modifier = Modifier.size(28.dp)) {
            Text(text = "⚠️", fontSize = 18.sp)
        }
        if (showWarningPopup) {
            Popup(onDismissRequest = { showWarningPopup = false }) {
                Surface(
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                    color = Color(WARNING_CONTAINER_COLOR_HEX),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 320.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "$nonEqualSpendCount gasto${if (nonEqualSpendCount > 1) "s" else ""} con reparto personalizado",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(WARNING_TEXT_COLOR_HEX)
                        )
                        Text(
                            text = "Las cifras de «Por pagador» muestran lo abonado, no la deuda real de cada persona. Consulta la pestaña Balances para ver los saldos exactos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(WARNING_TEXT_COLOR_HEX)
                        )
                        TextButton(onClick = { showWarningPopup = false }, modifier = Modifier.align(Alignment.End)) {
                            Text(text = "Entendido", color = Color(WARNING_TEXT_COLOR_HEX), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementNetRow(
    fromName: String,
    toName: String,
    netAmount: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(DivvyUpTokens.IconMd))
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(fromName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                }
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(toName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(text = "${netAmount.fmt2()} $currency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
