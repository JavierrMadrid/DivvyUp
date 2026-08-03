package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.divvyup.application.AnalyticsExportData
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.integration.ui.components.AppTabsRow
import com.example.divvyup.integration.ui.components.AppTopBar
import com.example.divvyup.integration.ui.components.PillFab
import com.example.divvyup.integration.ui.components.TopBarVariant
import com.example.divvyup.integration.ui.theme.*
import com.example.divvyup.integration.ui.viewmodel.GroupDetailTab
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import com.example.divvyup.integration.ui.theme.DivvyUpTokens

internal val MES_CORTO =
    listOf("", "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
internal val MES_NOMBRES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
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
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    onAddSpend: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSettleUp: () -> Unit,
    onOpenSpend: (spendId: Long) -> Unit = {},
    onShareText: (String) -> Unit = {},
    onSharePdf: (AnalyticsExportData) -> Unit = {},
    onShareExcel: (AnalyticsExportData) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh on app resume — si la app vuelve de background y esta pantalla
    // está activa, los datos cacheados (gastos, balances, actividad) pueden estar
    // stale. Sin este observer, la UI seguía mostrando "no hay gastos" cuando
    // sí los había (cache TTL 1 min de CachedSpendRepository expirado).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Consumir texto de exportación pendiente → disparar share del sistema
    LaunchedEffect(uiState.pendingExportText) {
        val text = uiState.pendingExportText ?: return@LaunchedEffect
        viewModel.consumeExportText()
        onShareText(text)
    }
    LaunchedEffect(uiState.pendingExportPdf) {
        val data = uiState.pendingExportPdf ?: return@LaunchedEffect
        viewModel.consumeExportPdf()
        onSharePdf(data)
    }
    LaunchedEffect(uiState.pendingExportExcel) {
        val data = uiState.pendingExportExcel ?: return@LaunchedEffect
        viewModel.consumeExportExcel()
        onShareExcel(data)
    }
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
            Column {
                AppTopBar(
                    title = uiState.group?.name ?: "Cargando...",
                    subtitle = uiState.group?.let {
                        "${uiState.participants.size} participantes - ${it.currency}"
                    },
                    variant = TopBarVariant.Gradient,
                    onBack = onBack,
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Ajustes del grupo",
                                tint = Color.White
                            )
                        }
                    }
                )
                AppTabsRow(
                    selectedIndex = uiState.selectedTab.ordinal,
                    onSelect = { idx -> viewModel.selectTab(GroupDetailTab.entries[idx]) },
                    tabLabels = listOf("Gastos", "Balances", "Analíticas", "Actividad")
                )
            }
        },
        floatingActionButton = {
            when (uiState.selectedTab) {
                GroupDetailTab.GASTOS -> PillFab(
                    onClick = onAddSpend,
                    icon = Icons.Default.Add,
                    label = "Nuevo gasto"
                )

                GroupDetailTab.BALANCES -> {}
                GroupDetailTab.ANALITICAS -> {}
                GroupDetailTab.ACTIVIDAD -> {}
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
                            spendPersonalImpact = uiState.spendPersonalImpact,
                            onEditSpend = { spend -> onOpenSpend(spend.id) },
                            onDeleteSpendsByIds = viewModel::deleteSpendsByIds,
                            onDeleteSpendsFiltered = viewModel::deleteSpendsFiltered,
                            isRefreshing = uiState.isLoading,
                            onRefresh = viewModel::loadAll
                        )

                    GroupDetailTab.BALANCES ->
                        BalanceTab(
                            balances = uiState.balances,
                            transfers = uiState.debtTransfers,
                            currency = uiState.group?.currency ?: "EUR",
                            onLiquidar = onOpenSettleUp
                        )

                    GroupDetailTab.ANALITICAS ->
                        AnalyticsTab(
                            spends = analyticsSpends,
                            categories = analyticsCategories,
                            participants = uiState.participants,
                            settlements = uiState.settlements,
                            spendSharesBySpend = uiState.spendSharesBySpend,
                            balances = uiState.balances,
                            currency = uiState.group?.currency ?: "EUR",
                            searchQuery = uiState.analyticsSearchQuery,
                            selectedCategories = uiState.analyticsSelectedCategories,
                            selectedParticipants = uiState.analyticsSelectedParticipants,
                            period = uiState.analyticsPeriod,
                            onSearchQueryChange = viewModel::setAnalyticsSearchQuery,
                            onCategoryToggle = viewModel::toggleAnalyticsCategory,
                            onParticipantToggle = viewModel::toggleAnalyticsParticipant,
                            onPeriodChange = viewModel::setAnalyticsPeriod,
                            onClearFilters = viewModel::clearAnalyticsFilters,
                            onExportText = { filteredSpends ->
                                viewModel.exportGroupText(
                                    filteredSpends
                                )
                            },
                            onExportCsv = { filteredSpends ->
                                viewModel.exportGroupCsv(
                                    filteredSpends
                                )
                            },
                            onExportPdf = { filteredSpends, periodLabel ->
                                viewModel.exportGroupPdf(
                                    filteredSpends,
                                    periodLabel
                                )
                            },
                            onExportExcel = { filteredSpends, periodLabel ->
                                viewModel.exportGroupExcel(
                                    filteredSpends,
                                    periodLabel
                                )
                            }
                        )

                    GroupDetailTab.ACTIVIDAD ->
                        ActivityTab(
                            activityLog = uiState.activityLog,
                            onRefresh = viewModel::loadAll
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

}
