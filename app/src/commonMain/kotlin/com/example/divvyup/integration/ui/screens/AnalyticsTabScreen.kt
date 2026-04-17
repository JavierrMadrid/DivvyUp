package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.integration.ui.components.AppFilterChip
import com.example.divvyup.integration.ui.components.AppSearchField
import com.example.divvyup.integration.ui.components.rememberAppFilterChipPalette
import com.example.divvyup.integration.ui.theme.*
import com.example.divvyup.integration.ui.viewmodel.AnalyticsPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

// --- Tab: Analíticas ---------------------------------------------------------

@OptIn(ExperimentalTime::class)
@Composable
internal fun AnalyticsTab(
    spends: List<Spend>,
    categories: List<Category>,
    participants: List<Participant>,
    settlements: List<Settlement>,
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
                val d = spend.date.toLocalDateTime(TimeZone.currentSystemDefault())
                when (period) {
                    is AnalyticsPeriod.Todo     -> true
                    is AnalyticsPeriod.PorMes   -> d.year == period.year && d.month == period.month
                    is AnalyticsPeriod.PorAnyo  -> d.year == period.year
                    is AnalyticsPeriod.PorRango -> d.date >= period.desde && d.date <= period.hasta
                }
            }
        }
    }

    val filtered by remember(periodFiltered, effectiveSelectedCategories, selectedParticipants, searchQuery) {
        derivedStateOf {
            periodFiltered.filter { spend ->
                val queryOk = searchQuery.isBlank() || spend.concept.contains(searchQuery, ignoreCase = true)
                val catOk = effectiveSelectedCategories.isEmpty() || (spend.categoryId != null && spend.categoryId in effectiveSelectedCategories)
                val participantOk = selectedParticipants.isEmpty() || spend.payerId in selectedParticipants
                queryOk && catOk && participantOk
            }
        }
    }

    val totalFiltered by remember(filtered) { derivedStateOf { filtered.sumOf { it.amount } } }

    // Gastos no equitativos en la selección actual → las cifras "por pagador" no reflejan deuda real
    val nonEqualSpendCount by remember(filtered) {
        derivedStateOf { filtered.count { it.splitType != SplitType.EQUAL } }
    }

    val byCategory by remember(filtered, categoryMap) {
        derivedStateOf {
            filtered.groupBy { it.categoryId }.map { (catId, list) ->
                val cat = catId?.let { categoryMap[it] }
                Triple(cat?.icon ?: "📦", cat?.name ?: "Sin categoría", list.sumOf { it.amount })
            }.sortedByDescending { it.third }
        }
    }

    val byPayer by remember(filtered, participantMap) {
        derivedStateOf {
            filtered.groupBy { it.payerId }.map { (payerId, list) ->
                Pair(participantMap[payerId]?.name ?: "Desconocido", list.sumOf { it.amount })
            }.sortedByDescending { it.second }
        }
    }

    val netSettlements by remember(settlements) {
        derivedStateOf {
            val pairNet = mutableMapOf<Pair<Long, Long>, Double>()
            for (s in settlements) {
                val a = s.fromParticipantId; val b = s.toParticipantId
                if (a < b) pairNet[a to b] = (pairNet[a to b] ?: 0.0) + s.amount
                else       pairNet[b to a] = (pairNet[b to a] ?: 0.0) - s.amount
            }
            pairNet.mapNotNull { (pair, net) ->
                when {
                    net > 0.005  -> Triple(pair.first, pair.second, net)
                    net < -0.005 -> Triple(pair.second, pair.first, -net)
                    else         -> null
                }
            }.sortedByDescending { it.third }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Buscador ──────────────────────────────────────────────────────
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
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = DivvyUpTokens.ControlHeight)
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
                        Icons.Default.FilterAltOff,
                        contentDescription = "Limpiar filtros",
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // ── Selector de período ───────────────────────────────────────────
        item {
            PeriodFilterSelector(period = period, currentYear = now.year, currentMonth = now.month, onPeriodChange = onPeriodChange)
        }

        // ── Chips de categoría ────────────────────────────────────────────
        if (categories.isNotEmpty()) {
            item {
                Text("Categoría", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        val isSelected = cat.id in selectedCategories
                        AppFilterChip(
                            label = "${cat.icon} ${cat.name}",
                            selected = isSelected,
                            selectedColor = chipPalette.selectedColor,
                            unselectedColor = chipPalette.unselectedColor,
                            unselectedTextColor = chipPalette.unselectedTextColor,
                            height = DivvyUpTokens.ChipHeight,
                            onClick = { onCategoryToggle(cat.id) }
                        )
                    }
                }
            }
        }

        // ── Chips de personas ─────────────────────────────────────────────
        if (participants.isNotEmpty()) {
            item {
                Spacer(Modifier.height(2.dp))
                Text("Persona", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(participants, key = { it.id }) { participant ->
                        val isSelected = participant.id in selectedParticipants
                        val avatarColor = participantAvatarPalette[participant.name.length % participantAvatarPalette.size]
                        Surface(
                            onClick = { onParticipantToggle(participant.id) },
                            shape = RoundedCornerShape(50.dp),
                            color = if (isSelected) avatarColor else chipPalette.unselectedColor,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(if (isSelected) Color.White.copy(alpha = 0.3f) else avatarColor), contentAlignment = Alignment.Center) {
                                    Text(participant.name.first().uppercaseChar().toString(), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                }
                                Text(participant.name, style = MaterialTheme.typography.labelMedium, color = if (isSelected) Color.White else chipPalette.unselectedTextColor, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }
                }
            }
        }

        // ── Separador ─────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))
        }


        // ── Resumen total ─────────────────────────────────────────────────
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = JungleGreen), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total gastado", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                        Text("${totalFiltered.fmt2()} $currency", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${filtered.size} gastos", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                        Text("promedio ${if (filtered.isNotEmpty()) (totalFiltered / filtered.size).fmt2() else "0.00"}", style = MaterialTheme.typography.bodySmall, color = JungleGreenLight)
                    }
                }
            }
        }

        // ── Gráfico por categoría ─────────────────────────────────────────
        if (byCategory.isNotEmpty()) {
            item {
                Text("Por categoría", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                DonutChartCard(entries = byCategory.map { (icon, name, total) -> DonutEntry(label = name, icon = icon, value = total.toFloat()) }, currency = currency)
            }
        }

        // ── Gráfico por pagador ───────────────────────────────────────────
        if (byPayer.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Por pagador",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    // Icono de advertencia cuando hay gastos con reparto no equitativo
                    if (nonEqualSpendCount > 0) {
                        var showWarningPopup by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showWarningPopup = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text("⚠️", fontSize = 18.sp)
                        }
                        if (showWarningPopup) {
                            androidx.compose.ui.window.Popup(
                                onDismissRequest = { showWarningPopup = false }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                                    color = Color(0xFFFFF3CD),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .widthIn(max = 320.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            "$nonEqualSpendCount gasto${if (nonEqualSpendCount > 1) "s" else ""} con reparto personalizado",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7C5200)
                                        )
                                        Text(
                                            "Las cifras de «Por pagador» muestran lo abonado, no la deuda real de cada persona. Consulta la pestaña Balances para ver los saldos exactos.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF7C5200)
                                        )
                                        TextButton(
                                            onClick = { showWarningPopup = false },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Entendido", color = Color(0xFF7C5200), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                HorizontalBarChartCard(entries = byPayer.map { (name, total) -> BarEntry(label = name, value = total.toFloat()) }, currency = currency, total = totalFiltered)
            }
        }

        // ── Resumen neto de liquidaciones ─────────────────────────────────
        if (settlements.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Resumen de liquidaciones", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
            }
            if (netSettlements.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = JungleGreen100, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("✅", fontSize = 22.sp)
                            Text("Todas las cuentas están saldadas", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = JungleGreenDark)
                        }
                    }
                }
            } else {
                items(netSettlements, key = { "net_${it.first}_${it.second}" }) { (fromId, toId, netAmount) ->
                    val fromName = participantMap[fromId]?.name ?: "Desconocido"
                    val toName = participantMap[toId]?.name ?: "Desconocido"
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(0.05f), spotColor = Color.Black.copy(0.08f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(JungleGreen100), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = JungleGreenDark, modifier = Modifier.size(20.dp))
                            }
                            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.errorContainer) {
                                    Text(fromName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(shape = RoundedCornerShape(50.dp), color = JungleGreen100) {
                                    Text(toName, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = JungleGreenDark)
                                }
                            }
                            Text("${netAmount.fmt2()} $currency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = JungleGreenMid)
                        }
                    }
                }
            }
        }

        // ── Lista de gastos filtrados (paginada) ──────────────────────────
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
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔍", fontSize = 36.sp)
                        Text("Sin resultados para los filtros aplicados", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ── Lista de gastos paginada (composable propio para poder usar rememberSaveable) ──

@Composable
private fun AnalyticsSpendList(
    filtered: List<Spend>,
    currency: String,
    categoryMap: Map<Long, Category>,
    participantMap: Map<Long, Participant>,
    searchQuery: String,
    selectedCategories: Set<Long>,
    selectedParticipants: Set<Long>,
    period: AnalyticsPeriod
) {
    val spendPageSize = 5
    var visibleSpendCount by rememberSaveable(
        filtered.size, searchQuery, selectedCategories, selectedParticipants, period
    ) { mutableStateOf(spendPageSize) }

    val visibleSpends = filtered.take(visibleSpendCount)
    val hasMore = visibleSpendCount < filtered.size

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Gastos (${visibleSpends.size} de ${filtered.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        visibleSpends.forEach { spend ->
            val cat = spend.categoryId?.let { categoryMap[it] }
            val payerName = participantMap[spend.payerId]?.name ?: "Desconocido"
            val dateFormatted = formatLocalDate(spend.date.toLocalDate())
            val isNonEqual = spend.splitType != SplitType.EQUAL
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(0.05f), spotColor = Color.Black.copy(0.08f))
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Text(cat?.icon ?: "📦", fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(spend.concept, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (isNonEqual) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFF3CD)
                                ) {
                                    Text(
                                        text = when (spend.splitType) {
                                            SplitType.PERCENTAGE -> "%"
                                            SplitType.CUSTOM     -> "✎"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7C5200),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text("$payerName · $dateFormatted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${spend.amount.fmt2()} $currency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (hasMore) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(
                    onClick = { visibleSpendCount += spendPageSize },
                    shape = RoundedCornerShape(50.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, JungleGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ExpandMore, contentDescription = null, tint = JungleGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mostrar más (${filtered.size - visibleSpendCount} restantes)", color = JungleGreen, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// --- Selector de período para analíticas -------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodFilterSelector(
    period: AnalyticsPeriod,
    currentYear: Int,
    currentMonth: Month,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipPalette = rememberAppFilterChipPalette(selectedColor = JungleGreen)
    val availableYears = (currentYear - 2..currentYear).toList().reversed()
    val periodControlBorderColor = if (isSystemInDarkTheme()) DarkTextBeige200 else MaterialTheme.colorScheme.outline
    var showDatePickerDesde by remember { mutableStateOf(false) }
    var showDatePickerHasta by remember { mutableStateOf(false) }
    val rangeDesde: LocalDate? = (period as? AnalyticsPeriod.PorRango)?.desde
    val rangeHasta: LocalDate? = (period as? AnalyticsPeriod.PorRango)?.hasta

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Período", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { val s = period is AnalyticsPeriod.Todo;   PeriodChip("Todo", s, chipPalette.selectedColor, chipPalette.unselectedColor, chipPalette.unselectedTextColor) { onPeriodChange(AnalyticsPeriod.Todo) } }
            item { val s = period is AnalyticsPeriod.PorMes; PeriodChip("Por mes", s, chipPalette.selectedColor, chipPalette.unselectedColor, chipPalette.unselectedTextColor) { if (!s) onPeriodChange(AnalyticsPeriod.PorMes(currentMonth, currentYear)) } }
            item { val s = period is AnalyticsPeriod.PorAnyo; PeriodChip("Por año", s, chipPalette.selectedColor, chipPalette.unselectedColor, chipPalette.unselectedTextColor) { if (!s) onPeriodChange(AnalyticsPeriod.PorAnyo(currentYear)) } }
            item {
                val s = period is AnalyticsPeriod.PorRango
                PeriodChip("Rango", s, chipPalette.selectedColor, chipPalette.unselectedColor, chipPalette.unselectedTextColor) {
                    if (!s) {
                        val today = System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                        onPeriodChange(AnalyticsPeriod.PorRango(desde = today, hasta = today))
                    }
                }
            }
        }

        when (period) {
            is AnalyticsPeriod.PorMes -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PeriodDropdown(options = Month.entries.map { it.number to MES_NOMBRES[it.number - 1] }, selected = period.month.number, onSelect = { onPeriodChange(AnalyticsPeriod.PorMes(Month(it), period.year)) }, modifier = Modifier.weight(1f))
                    PeriodDropdown(options = availableYears.map { it to it.toString() }, selected = period.year, onSelect = { onPeriodChange(AnalyticsPeriod.PorMes(period.month, it)) }, modifier = Modifier.weight(1f))
                }
            }
            is AnalyticsPeriod.PorAnyo -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PeriodDropdown(options = availableYears.map { it to it.toString() }, selected = period.year, onSelect = { onPeriodChange(AnalyticsPeriod.PorAnyo(it)) }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            is AnalyticsPeriod.PorRango -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showDatePickerDesde = true }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface), border = androidx.compose.foundation.BorderStroke(1.dp, periodControlBorderColor)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                        Text(if (rangeDesde != null) formatLocalDate(rangeDesde) else "Desde", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = { showDatePickerHasta = true }, modifier = Modifier.weight(1f).height(44.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface), border = androidx.compose.foundation.BorderStroke(1.dp, periodControlBorderColor)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                        Text(if (rangeHasta != null) formatLocalDate(rangeHasta) else "Hasta", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            else -> {}
        }
    }

    if (showDatePickerDesde) {
        val initMillis: Long? = if (rangeDesde != null) localDateToMillis(rangeDesde) else null
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDesde = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val nuevaDesde = millisToLocalDate(millis)
                        val hasta = rangeHasta ?: nuevaDesde
                        onPeriodChange(AnalyticsPeriod.PorRango(desde = nuevaDesde, hasta = if (hasta < nuevaDesde) nuevaDesde else hasta))
                    }
                    showDatePickerDesde = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerDesde = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state, colors = appDatePickerColors()) }
    }

    if (showDatePickerHasta) {
        val initMillis: Long? = if (rangeHasta != null) localDateToMillis(rangeHasta) else null
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerHasta = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val nuevaHasta = millisToLocalDate(millis)
                        val desde = rangeDesde ?: nuevaHasta
                        onPeriodChange(AnalyticsPeriod.PorRango(desde = if (desde > nuevaHasta) nuevaHasta else desde, hasta = nuevaHasta))
                    }
                    showDatePickerHasta = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerHasta = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state, colors = appDatePickerColors()) }
    }
}

// --- Chip de período ---------------------------------------------------------

@Composable
internal fun PeriodChip(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    unselectedTextColor: Color,
    onClick: () -> Unit
) {
    AppFilterChip(
        label = label,
        selected = isSelected,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        unselectedTextColor = unselectedTextColor,
        height = DivvyUpTokens.ChipHeight,
        onClick = onClick
    )
}

// --- Dropdown de período -----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodDropdown(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: ""
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedLabel, onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp).menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = appOutlinedTextFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label, style = MaterialTheme.typography.bodyMedium) }, onClick = { onSelect(value); expanded = false })
            }
        }
    }
}

// --- Gráficas de analíticas --------------------------------------------------

private val chartPaletteTokens = listOf(
    JungleGreen, BarkBrown, MossGold, JungleGreenMid,
    Amber, BarkBrownLight, JungleGreenLight, Soil, BarkBrownDark, JungleGreenDark
)

@Composable
private fun rememberChartPalette(): List<Color> {
    val isDark = MaterialTheme.colorScheme.surface == DarkJungleSurface || MaterialTheme.colorScheme.background.red < 0.3f
    return if (isDark) listOf(JungleGreenLight, BarkBrownLight, Amber, JungleGreenMid, MossGold, JungleGreen, BarkBrown, JungleGreenDark, Soil, BarkBrownDark)
    else chartPaletteTokens
}

@Composable
internal fun DonutChartCard(entries: List<DonutEntry>, currency: String, modifier: Modifier = Modifier) {
    val chartPalette = rememberChartPalette()
    val totalCalculo = entries.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(0.001f)
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(0.05f), spotColor = Color.Black.copy(0.08f))) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(130.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = size.width * 0.18f
                        val radius = (size.minDimension - strokeW) / 2f
                        val topLeft = Offset(strokeW / 2f, strokeW / 2f)
                        val arcSize = Size(radius * 2f, radius * 2f)
                        var startAngle = -90f
                        entries.forEachIndexed { i, entry ->
                            val sweep = (entry.value / totalCalculo) * 360f
                            drawArc(color = chartPalette[i % chartPalette.size], startAngle = startAngle, sweepAngle = sweep - 2f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                            startAngle += sweep
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${entries.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("categorías", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.take(5).forEachIndexed { i, entry ->
                        val pct = (entry.value / totalCalculo) * 100f
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(chartPalette[i % chartPalette.size]))
                            Text(entry.icon, fontSize = 13.sp, modifier = Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${entry.value.toDouble().fmt2()} $currency · ${(pct.toDouble()).fmt2()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (entries.size > 5) Text("+${entries.size - 5} más", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun HorizontalBarChartCard(entries: List<BarEntry>, currency: String, total: Double, modifier: Modifier = Modifier) {
    val chartPalette = rememberChartPalette()
    val maxValue = entries.maxOfOrNull { it.value } ?: 1f
    val minValue = entries.minOfOrNull { it.value } ?: 0f
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(0.05f), spotColor = Color.Black.copy(0.08f))) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            val errorColor = MaterialTheme.colorScheme.error
            entries.forEachIndexed { i, entry ->
                val fraction = (entry.value / maxValue).coerceIn(0f, 1f)
                val pct = if (total > 0) (entry.value / total.toFloat()) * 100f else 0f
                val isMinBar = entries.size > 1 && entry.value == minValue
                val barColor = if (isMinBar) errorColor else chartPalette[i % chartPalette.size]
                val avatarColor = participantAvatarPalette[entry.label.length % participantAvatarPalette.size]
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) {
                            Text(entry.label.first().uppercaseChar().toString(), fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 13.sp)
                        }
                        Text(entry.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${entry.value.toDouble().fmt2()} $currency", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = barColor)
                        Text("${pct.toDouble().fmt2()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(RoundedCornerShape(50.dp)).background(brush = Brush.horizontalGradient(listOf(barColor, barColor.copy(alpha = 0.7f)))))
                    }
                }
            }
        }
    }
}
