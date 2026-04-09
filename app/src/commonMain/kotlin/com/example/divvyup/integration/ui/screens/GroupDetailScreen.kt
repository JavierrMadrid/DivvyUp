package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.integration.ui.theme.*
import com.example.divvyup.integration.ui.viewmodel.GroupDetailTab
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import com.example.divvyup.integration.ui.theme.DivvyUpTokens

internal val participantAvatarPalette = listOf(
    JungleGreen, JungleGreenDark, BarkBrown,
    MossGold, Soil, JungleGreenMid,
    BarkBrownDark, Amber
)

internal val MES_CORTO = listOf("", "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
internal val MES_NOMBRES = listOf(
    "Enero","Febrero","Marzo","Abril","Mayo","Junio",
    "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"
)

internal const val SETTLEMENT_CATEGORY_NAME = "Liquidación"
internal const val SETTLEMENT_SPEND_NOTE_PREFIX = "__settlement_id:"

internal fun Instant.toLocal() = toLocalDateTime(TimeZone.currentSystemDefault())
internal fun Instant.toLocalDate() = toLocal().date

internal fun Category.isSettlementCategory(): Boolean =
    name.equals(SETTLEMENT_CATEGORY_NAME, ignoreCase = true)

internal fun Spend.isSettlementSpend(settlementCategoryIds: Set<Long>): Boolean =
    (categoryId != null && categoryId in settlementCategoryIds) ||
        notes.startsWith(SETTLEMENT_SPEND_NOTE_PREFIX)

/** Formatea un Double con 2 decimales sin usar String.format (KMP-compatible). */
internal fun Double.fmt2(): String {
    val r = kotlin.math.round(this * 100) / 100.0
    return "${r.toLong()}.${(kotlin.math.abs(r % 1) * 100).toLong().toString().padStart(2, '0')}"
}

internal fun formatLocalDate(date: LocalDate): String =
    "${date.day} ${MES_CORTO[date.month.number]} ${date.year}"

internal fun localDateToMillis(date: LocalDate): Long =
    date.toEpochDays() * 24L * 60L * 60L * 1000L

internal fun millisToLocalDate(millis: Long): LocalDate {
    val epochDays = (millis / (24L * 60L * 60L * 1000L)).toInt()
    return LocalDate.fromEpochDays(epochDays)
}

@Composable
internal fun appDatePickerColors() = DatePickerDefaults.colors(
    selectedDayContainerColor = if (isSystemInDarkTheme()) BarkBrown else JungleGreen,
    selectedDayContentColor = Color.White,
    todayDateBorderColor = if (isSystemInDarkTheme()) BarkBrownLight else JungleGreen,
    todayContentColor = MaterialTheme.colorScheme.onSurface,
    selectedYearContainerColor = if (isSystemInDarkTheme()) BarkBrown else JungleGreen,
    selectedYearContentColor = Color.White
)

@Composable
internal fun FintechFab(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    onAddSpend: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val settlementCategoryIds by remember(uiState.categories) {
        derivedStateOf {
            uiState.categories
                .filter { it.isSettlementCategory() }
                .map { it.id }
                .toSet()
        }
    }
    val analyticsCategories by remember(uiState.categories) {
        derivedStateOf { uiState.categories.filterNot { it.isSettlementCategory() } }
    }
    val analyticsSpends by remember(uiState.spends, settlementCategoryIds) {
        derivedStateOf {
            uiState.spends.filterNot { it.isSettlementSpend(settlementCategoryIds) }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Header estilo fintech con gradiente navy
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(JungleGreen, JungleGreenDark)
                        )
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.group?.name ?: "Cargando..¦",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        uiState.group?.let { group ->
                            Text(
                                text = "${uiState.participants.size} participantes · ${group.currency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Ajustes del grupo",
                            tint = Color.White
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GroupDetailTab.entries.forEach { tab ->
                        val isSelected = uiState.selectedTab == tab
                        Surface(
                            onClick = { viewModel.selectTab(tab) },
                            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                            color = if (isSelected) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                            contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = when (tab) {
                                        GroupDetailTab.GASTOS     -> "Gastos"
                                        GroupDetailTab.BALANCES   -> "Balances"
                                        GroupDetailTab.ANALITICAS -> "Analíticas"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            when (uiState.selectedTab) {
                GroupDetailTab.GASTOS -> FintechFab(
                    onClick = onAddSpend,
                    icon = Icons.Default.Add,
                    label = "Nuevo gasto"
                )
                GroupDetailTab.BALANCES -> {}
                GroupDetailTab.ANALITICAS -> {} // sin FAB
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = JungleGreenMid,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                when (uiState.selectedTab) {
                    GroupDetailTab.GASTOS ->
                        SpendTab(
                            spends = uiState.spends,
                            participants = uiState.participants,
                            categories = uiState.categories,
                            currency = uiState.group?.currency ?: "EUR",
                            onEditSpend = viewModel::prepareEditSpend,
                            onDeleteSpendsByIds = viewModel::deleteSpendsByIds,
                            onDeleteSpendsFiltered = viewModel::deleteSpendsFiltered
                        )
                    GroupDetailTab.BALANCES ->
                        BalanceTab(
                            balances = uiState.balances,
                            transfers = uiState.debtTransfers,
                            currency = uiState.group?.currency ?: "EUR",
                            onLiquidar = viewModel::showSettleUpDialog
                        )
                    GroupDetailTab.ANALITICAS ->
                        AnalyticsTab(
                            spends = analyticsSpends,
                            categories = analyticsCategories,
                            participants = uiState.participants,
                            settlements = uiState.settlements,
                            currency = uiState.group?.currency ?: "EUR",
                            searchQuery = uiState.analyticsSearchQuery,
                            selectedCategories = uiState.analyticsSelectedCategories,
                            selectedParticipants = uiState.analyticsSelectedParticipants,
                            period = uiState.analyticsPeriod,
                            onSearchQueryChange = viewModel::setAnalyticsSearchQuery,
                            onCategoryToggle = viewModel::toggleAnalyticsCategory,
                            onParticipantToggle = viewModel::toggleAnalyticsParticipant,
                            onPeriodChange = viewModel::setAnalyticsPeriod,
                            onClearFilters = viewModel::clearAnalyticsFilters
                        )
                }
            }

            // Error snackbar
            uiState.error?.let { errorMsg ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(20.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
                ) { Text(errorMsg) }
            }
        }
    }

    // Solo SettleUp mantiene dialog (no afecta navegación principal)
    if (uiState.showSettleUpDialog) {
        SettleUpDialog(
            transfers = uiState.debtTransfers,
            currency = uiState.group?.currency ?: "EUR",
            onConfirm = { selected ->
                viewModel.createSettlementsForTransfers(selected)
            },
            onDismiss = viewModel::hideSettleUpDialog
        )
    }
}

// -- Modelos de datos para gráficas (compartidos por AnalyticsTabScreen) -------
internal data class DonutEntry(val label: String, val icon: String, val value: Float)
internal data class BarEntry(val label: String, val value: Float)


