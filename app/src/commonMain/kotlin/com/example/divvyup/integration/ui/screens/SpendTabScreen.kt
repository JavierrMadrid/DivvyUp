package com.example.divvyup.integration.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.integration.ui.components.AppFilterChip
import com.example.divvyup.integration.ui.components.AppFilterChipRow
import com.example.divvyup.integration.ui.components.AppFilterLabel
import com.example.divvyup.integration.ui.components.AppSearchField
import com.example.divvyup.integration.ui.components.rememberAppFilterChipPalette
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreen100
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import kotlinx.datetime.LocalDate
import kotlin.time.Clock.System
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val DEFAULT_UNCATEGORIZED_ICON = "📦"

// ── Opciones de tiempo para borrado avanzado ──────────────────────────────────
internal enum class SpendDeleteTimeOption(val label: String) {
    TODOS("Todos"),
    ANTES_ULTIMA_SEMANA("Anteriores a la última semana"),
    ANTES_ULTIMO_MES("Anteriores al último mes"),
    ANTES_TRES_MESES("Anteriores a los últimos 3 meses"),
    ANTES_ULTIMO_ANYO("Anteriores al último año")
}

// --- Tab: Gastos -------------------------------------------------------------

@OptIn(ExperimentalTime::class)
@Composable
internal fun SpendTab(
    spends: List<Spend>,
    participants: List<Participant>,
    categories: List<Category>,
    currency: String,
    spendPersonalImpact: Map<Long, Double> = emptyMap(),
    onEditSpend: (Spend) -> Unit,
    onDeleteSpendsByIds: (Set<Long>) -> Unit,
    onDeleteSpendsFiltered: (categoryId: Long?, payerId: Long?, beforeInstant: Instant?) -> Unit,
    // Comentarios
    commentsForSpend: List<com.example.divvyup.domain.model.SpendComment> = emptyList(),
    commentSpendId: Long? = null,
    isSendingComment: Boolean = false,
    myParticipantId: Long? = null,
    onOpenComments: (Long) -> Unit = {},
    onCloseComments: () -> Unit = {},
    onSendComment: (String) -> Unit = {},
    onDeleteComment: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAdvancedDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showFiltersDialog by rememberSaveable { mutableStateOf(false) }
    var spendSearchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectedParticipantIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectedFromDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedToDate by remember { mutableStateOf<LocalDate?>(null) }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedSpendIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }
    val categoriesForAdvancedDelete by remember(categories) {
        derivedStateOf { categories.filterNot { it.isSettlementCategory() } }
    }

    if (spends.isEmpty()) {
        SpendEmptyState(modifier)
        return
    }

    val participantMap by remember(participants) { derivedStateOf { participants.associateBy { it.id } } }
    val categoryMap    by remember(categories)   { derivedStateOf { categories.associateBy { it.id } } }
    val settlementCategoryIds by remember(categories) {
        derivedStateOf {
            categories
                .filter { it.isSettlementCategory() }
                .map { it.id }
                .toSet()
        }
    }
    val filteredSpends by remember(
        spends, spendSearchQuery, selectedCategoryIds,
        selectedParticipantIds, selectedFromDate, selectedToDate
    ) {
        derivedStateOf {
            val query = spendSearchQuery.trim()
            spends.filter { spend ->
                val matchesQuery = query.isEmpty() || spend.concept.contains(query, ignoreCase = true)
                val matchesCategory = selectedCategoryIds.isEmpty() ||
                    (spend.categoryId != null && spend.categoryId in selectedCategoryIds)
                val matchesParticipant = selectedParticipantIds.isEmpty() || spend.payerId in selectedParticipantIds
                val spendDate = spend.date.toLocalDate()
                val matchesFrom = selectedFromDate == null || spendDate >= selectedFromDate!!
                val matchesTo = selectedToDate == null || spendDate <= selectedToDate!!
                matchesQuery && matchesCategory && matchesParticipant && matchesFrom && matchesTo
            }
        }
    }
    val hasActiveFilters = selectedCategoryIds.isNotEmpty() || selectedParticipantIds.isNotEmpty() ||
        selectedFromDate != null || selectedToDate != null

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppSearchField(
                        value = spendSearchQuery,
                        onValueChange = { spendSearchQuery = it },
                        placeholder = "Buscar gasto",
                        onClear = { spendSearchQuery = "" },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = DivvyUpTokens.ControlHeight)
                    )
                    IconButton(
                        onClick = { showFiltersDialog = true },
                        modifier = Modifier.size(DivvyUpTokens.ControlHeight)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filtros",
                            tint = if (hasActiveFilters) JungleGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedSpendIds = emptySet()
                            } else {
                                isSelectionMode = true
                                selectedSpendIds = filteredSpends.map { it.id }.toSet()
                            }
                        },
                        modifier = Modifier.size(DivvyUpTokens.ControlHeight)
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Selección múltiple",
                            tint = if (isSelectionMode) JungleGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (filteredSpends.isEmpty()) {
                item {
                    Text(
                        text = "No hay gastos que coincidan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                return@LazyColumn
            }

            items(filteredSpends, key = { it.id }) { spend ->
                val isSettlementMirroredSpend = spend.isSettlementSpend(settlementCategoryIds)
                    SpendCard(
                        spend = spend,
                        payerName = participantMap[spend.payerId]?.name ?: "Desconocido",
                        categoryIcon = spend.categoryId?.let { categoryMap[it]?.icon } ?: DEFAULT_UNCATEGORIZED_ICON,
                        categoryName = spend.categoryId?.let { categoryMap[it]?.name },
                        currency = currency,
                        personalImpact = spendPersonalImpact[spend.id],
                        isSelectionMode = isSelectionMode,
                        isSelected = spend.id in selectedSpendIds,
                        onClick = {
                            if (isSelectionMode) {
                                selectedSpendIds = if (spend.id in selectedSpendIds)
                                    selectedSpendIds - spend.id else selectedSpendIds + spend.id
                            } else if (!isSettlementMirroredSpend) {
                                onEditSpend(spend)
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) isSelectionMode = true
                            selectedSpendIds = if (spend.id in selectedSpendIds)
                                selectedSpendIds - spend.id else selectedSpendIds + spend.id
                        },
                        onOpenComments = if (!isSettlementMirroredSpend) {
                            { onOpenComments(spend.id) }
                        } else null
                    )
            }
        }

        // FAB borrado — BottomStart
        FloatingActionButton(
            onClick = {
                if (isSelectionMode) {
                    if (selectedSpendIds.isNotEmpty()) showDeleteSelectedConfirm = true
                } else {
                    showAdvancedDeleteDialog = true
                }
            },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
            containerColor = JungleGreen,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 20.dp, bottom = 16.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                    ambientColor = JungleGreen.copy(alpha = 0.25f),
                    spotColor = JungleGreen.copy(alpha = 0.4f)
                )
        ) {
            if (isSelectionMode) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar seleccionados", modifier = Modifier.size(20.dp))
                    Text("${selectedSpendIds.size}", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            } else {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Borrado avanzado", modifier = Modifier.size(22.dp))
            }
        }
    }


    if (showFiltersDialog) {
        SpendListFiltersDialog(            participants = participants,
            categories = categories,
            selectedCategoryIds = selectedCategoryIds,
            selectedParticipantIds = selectedParticipantIds,
            selectedFromDate = selectedFromDate,
            selectedToDate = selectedToDate,
            onApply = { newCategories, newParticipants, newFrom, newTo ->
                selectedCategoryIds = newCategories
                selectedParticipantIds = newParticipants
                selectedFromDate = newFrom
                selectedToDate = newTo
                showFiltersDialog = false
            },
            onClear = {
                selectedCategoryIds = emptySet()
                selectedParticipantIds = emptySet()
                selectedFromDate = null
                selectedToDate = null
            },
            onDismiss = { showFiltersDialog = false }
        )
    }

    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
            title = { Text("Borrar gastos seleccionados", fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar ${selectedSpendIds.size} gasto(s) seleccionado(s)?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSpendsByIds(selectedSpendIds)
                        selectedSpendIds = emptySet()
                        isSelectionMode = false
                        showDeleteSelectedConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteSelectedConfirm = false }) { Text("Cancelar") } }
        )
    }

    if (showAdvancedDeleteDialog) {
        SpendAdvancedDeleteDialog(
            participants = participants,
            categories = categoriesForAdvancedDelete,
            onConfirm = { catId, payerId, beforeInstant ->
                onDeleteSpendsFiltered(catId, payerId, beforeInstant)
                showAdvancedDeleteDialog = false
            },
            onDismiss = { showAdvancedDeleteDialog = false }
        )
    }
}

// --- Estado vacío de gastos --------------------------------------------------

@Composable
internal fun SpendEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Text("💸", fontSize = 48.sp)
            Text("Sin gastos todavía", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Pulsa el botón para añadir el primer gasto",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- SpendCard ---------------------------------------------------------------

@Composable
internal fun SpendCard(
    spend: Spend,
    payerName: String,
    categoryIcon: String,
    categoryName: String?,
    currency: String,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    personalImpact: Double? = null,
    onOpenComments: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormatted = remember(spend.date) { formatLocalDate(spend.date.toLocalDate()) }

    // Colores del badge personal
    val impactColor: Color? = when {
        personalImpact == null -> null
        personalImpact > 0.005 -> Color(0xFF16A34A)  // verde — le deben
        personalImpact < -0.005 -> Color(0xFFDC2626)  // rojo — debe
        else -> null                                    // cero exacto → no mostrar
    }
    val impactSign = if ((personalImpact ?: 0.0) >= 0) "+" else ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) JungleGreen else Color.Transparent,
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard)
            )
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(if (isSelected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
            }
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) { Text(categoryIcon, fontSize = 22.sp) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    spend.concept,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(DivvyUpTokens.RadiusPill), color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(payerName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    categoryName?.let { catName ->
                        Surface(shape = RoundedCornerShape(DivvyUpTokens.RadiusPill), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(
                                catName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${spend.amount.fmt2()} $currency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                // Badge de impacto personal
                if (impactColor != null && personalImpact != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = impactColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$impactSign${personalImpact.fmt2()} $currency",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = impactColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- Diálogo filtros de gastos -----------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpendListFiltersDialog(
    participants: List<Participant>,
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    selectedParticipantIds: Set<Long>,
    selectedFromDate: LocalDate?,
    selectedToDate: LocalDate?,
    onApply: (Set<Long>, Set<Long>, LocalDate?, LocalDate?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var localCategoryIds by remember { mutableStateOf(selectedCategoryIds) }
    var localParticipantIds by remember { mutableStateOf(selectedParticipantIds) }
    var localFromDate by remember { mutableStateOf(selectedFromDate) }
    var localToDate by remember { mutableStateOf(selectedToDate) }
    var showDatePickerDesde by remember { mutableStateOf(false) }
    var showDatePickerHasta by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = { Text("Filtros", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (categories.isNotEmpty()) {
                    AppFilterLabel("Categoría")
                    AppFilterChipRow(items = categories) { category ->
                        AppFilterChip(
                            label = "${category.icon} ${category.name}",
                            selected = category.id in localCategoryIds,
                            selectedColor = JungleGreen
                        ) {
                            localCategoryIds = if (category.id in localCategoryIds)
                                localCategoryIds - category.id else localCategoryIds + category.id
                        }
                    }
                }
                if (participants.isNotEmpty()) {
                    AppFilterLabel("Persona")
                    AppFilterChipRow(items = participants) { participant ->
                        AppFilterChip(
                            label = participant.name,
                            selected = participant.id in localParticipantIds,
                            selectedColor = JungleGreen
                        ) {
                            localParticipantIds = if (participant.id in localParticipantIds)
                                localParticipantIds - participant.id else localParticipantIds + participant.id
                        }
                    }
                }
                AppFilterLabel("Rango de fechas")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showDatePickerDesde = true },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (localFromDate != null) formatLocalDate(localFromDate!!) else "Desde")
                    }
                    OutlinedButton(
                        onClick = { showDatePickerHasta = true },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (localToDate != null) formatLocalDate(localToDate!!) else "Hasta")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(localCategoryIds, localParticipantIds, localFromDate, localToDate) }) {
                Text("Aplicar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    localCategoryIds = emptySet()
                    localParticipantIds = emptySet()
                    localFromDate = null
                    localToDate = null
                    onClear()
                }) { Text("Limpiar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )

    if (showDatePickerDesde) {
        val initMillis = localFromDate?.let { localDateToMillis(it) }
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerDesde = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val newFrom = millisToLocalDate(millis)
                        localFromDate = newFrom
                        if (localToDate != null && localToDate!! < newFrom) localToDate = newFrom
                    }
                    showDatePickerDesde = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerDesde = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state, colors = appDatePickerColors()) }
    }

    if (showDatePickerHasta) {
        val initMillis = localToDate?.let { localDateToMillis(it) }
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePickerHasta = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val newTo = millisToLocalDate(millis)
                        localToDate = newTo
                        if (localFromDate != null && localFromDate!! > newTo) localFromDate = newTo
                    }
                    showDatePickerHasta = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerHasta = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state, colors = appDatePickerColors()) }
    }
}

// --- Diálogo borrado avanzado ------------------------------------------------

@Composable
internal fun SpendAdvancedDeleteDialog(
    participants: List<Participant>,
    categories: List<Category>,
    onConfirm: (categoryId: Long?, payerId: Long?, beforeInstant: Instant?) -> Unit,
    onDismiss: () -> Unit
) {
    val chipPalette = rememberAppFilterChipPalette(selectedColor = JungleGreen)
    val chipUnselectedColor = chipPalette.unselectedColor
    val chipUnselectedTextColor = chipPalette.unselectedTextColor
    val summaryContainerColor = chipPalette.summaryContainerColor

    var selectedCategory    by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedParticipant by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedTime        by rememberSaveable { mutableStateOf(SpendDeleteTimeOption.TODOS) }

    fun beforeInstant(): Instant? {
        val daysBack = when (selectedTime) {
            SpendDeleteTimeOption.ANTES_ULTIMA_SEMANA -> 7
            SpendDeleteTimeOption.ANTES_ULTIMO_MES    -> 30
            SpendDeleteTimeOption.ANTES_TRES_MESES    -> 90
            SpendDeleteTimeOption.ANTES_ULTIMO_ANYO   -> 365
            SpendDeleteTimeOption.TODOS               -> return null
        }
        return System.now() - daysBack.days
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Text("Borrar gastos", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppFilterLabel("Período")
                AppFilterChipRow(SpendDeleteTimeOption.entries) { opt ->
                    AppFilterChip(
                        label = opt.label,
                        selected = selectedTime == opt,
                        selectedColor = JungleGreen,
                        unselectedColor = chipUnselectedColor,
                        unselectedTextColor = chipUnselectedTextColor
                    ) { selectedTime = opt }
                }
                HorizontalDivider()
                if (categories.isNotEmpty()) {
                    AppFilterLabel("Categoría (opcional)")
                    AppFilterChipRow(
                        items = categories,
                        leadingAllLabel = "Todas",
                        onLeadingAllClick = { selectedCategory = null },
                        isLeadingAllSelected = selectedCategory == null
                    ) { cat ->
                        AppFilterChip(
                            label = "${cat.icon} ${cat.name}",
                            selected = selectedCategory == cat.id,
                            selectedColor = JungleGreen,
                            unselectedColor = chipUnselectedColor,
                            unselectedTextColor = chipUnselectedTextColor
                        ) {
                            selectedCategory = if (selectedCategory == cat.id) null else cat.id
                        }
                    }
                }
                if (participants.isNotEmpty()) {
                    AppFilterLabel("Persona (opcional)")
                    AppFilterChipRow(
                        items = participants,
                        leadingAllLabel = "Todos",
                        onLeadingAllClick = { selectedParticipant = null },
                        isLeadingAllSelected = selectedParticipant == null
                    ) { p ->
                        AppFilterChip(
                            label = p.name,
                            selected = selectedParticipant == p.id,
                            selectedColor = JungleGreen,
                            unselectedColor = chipUnselectedColor,
                            unselectedTextColor = chipUnselectedTextColor
                        ) {
                            selectedParticipant = if (selectedParticipant == p.id) null else p.id
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(DivvyUpTokens.RadiusControl), color = summaryContainerColor) {
                    Text(
                        buildString {
                            append("Se borrarán los gastos")
                            if (selectedTime != SpendDeleteTimeOption.TODOS) append(" ${selectedTime.label.lowercase()}")
                            if (selectedCategory    != null) append(" de la categoría seleccionada")
                            if (selectedParticipant != null) append(" pagados por la persona seleccionada")
                            append(". Esta acción no se puede deshacer.")
                        },
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCategory, selectedParticipant, beforeInstant()) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White)
            ) { Text("Borrar gastos", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ...existing code...
