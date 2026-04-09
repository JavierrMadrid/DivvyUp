@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.example.divvyup.integration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvyup.application.CategoryService
import com.example.divvyup.application.GroupService
import com.example.divvyup.application.SettlementService
import com.example.divvyup.application.SpendService
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.DebtTransfer
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.repository.ParticipantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.time.Instant

// Filtro de período para analíticas
sealed class AnalyticsPeriod {
    object Todo : AnalyticsPeriod()
    data class PorMes(val month: Month, val year: Int) : AnalyticsPeriod()
    data class PorAnyo(val year: Int) : AnalyticsPeriod()
    data class PorRango(val desde: LocalDate, val hasta: LocalDate) : AnalyticsPeriod()
}

data class GroupDetailUiState(
    val group: Group? = null,
    val participants: List<Participant> = emptyList(),
    val spends: List<Spend> = emptyList(),
    val categories: List<Category> = emptyList(),
    val balances: List<ParticipantBalance> = emptyList(),
    val debtTransfers: List<DebtTransfer> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val selectedTab: GroupDetailTab = GroupDetailTab.GASTOS,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Dialogs
    val showAddSpendDialog: Boolean = false,
    val showAddParticipantDialog: Boolean = false,
    val showSettleUpDialog: Boolean = false,
    val spendToEdit: Spend? = null,
    val sharesForEditedSpend: List<SpendShare> = emptyList(),
    // Señales de navegación
    val navigateToSpendScreen: Boolean = false,   // true → navegar a AddSpendScreen
    val spendSaved: Boolean = false,               // true → volver desde AddSpendScreen
    // Analíticas — filtros
    val analyticsSearchQuery: String = "",
    val analyticsSelectedCategories: Set<Long> = emptySet(),
    val analyticsSelectedParticipants: Set<Long> = emptySet(),
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.Todo,
    // Ajustes del grupo
    val defaultSplitPercentages: Map<Long, Double> = emptyMap()  // participantId → %
)

enum class GroupDetailTab { GASTOS, BALANCES, ANALITICAS }

class GroupDetailViewModel(
    private val groupId: Long,
    private val groupService: GroupService,
    private val spendService: SpendService,
    private val settlementService: SettlementService,
    private val categoryService: CategoryService,
    private val participantRepository: ParticipantRepository
) : ViewModel() {

    private companion object {
        const val SETTLEMENT_NOTE_PREFIX = "__settlement_id:"
    }

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val group        = groupService.getGroup(groupId)
                val participants = participantRepository.getByGroup(groupId)
                val spends       = spendService.getSpends(groupId)
                val categories   = categoryService.getCategories(groupId)
                val balances     = settlementService.getBalances(groupId)
                val settlements  = settlementService.getSettlements(groupId)
                val transfers    = settlementService.simplifyDebts(balances)

                _uiState.update {
                    it.copy(
                        group = group,
                        participants = participants,
                        spends = spends,
                        categories = categories,
                        balances = balances,
                        debtTransfers = transfers,
                        settlements = settlements,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // --- Tabs ---
    fun selectTab(tab: GroupDetailTab) = _uiState.update { it.copy(selectedTab = tab) }

    // --- Participantes ---
    fun showAddParticipantDialog() = _uiState.update { it.copy(showAddParticipantDialog = true) }
    fun hideAddParticipantDialog() = _uiState.update { it.copy(showAddParticipantDialog = false) }

    fun addParticipant(name: String, email: String?) {
        viewModelScope.launch {
            try {
                participantRepository.create(
                    Participant(groupId = groupId, name = name.trim(), email = email?.trim())
                )
                _uiState.update { it.copy(showAddParticipantDialog = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteParticipant(participantId: Long) {
        viewModelScope.launch {
            try {
                participantRepository.delete(participantId)
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // --- Gastos ---
    /** Prepara estado para crear un gasto nuevo y señala navegación a AddSpendScreen. */
    fun prepareNewSpend() = _uiState.update {
        it.copy(
            spendToEdit = null,
            sharesForEditedSpend = emptyList(),
            navigateToSpendScreen = true,
            spendSaved = false
        )
    }

    /** Carga las shares del gasto y señala navegación a AddSpendScreen cuando estén listas. */
    fun prepareEditSpend(spend: Spend) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, spendSaved = false) }
            try {
                val shares = spendService.getSharesBySpend(spend.id)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        spendToEdit = spend,
                        sharesForEditedSpend = shares,
                        navigateToSpendScreen = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** Llamar desde AppNavigation justo después de navegar a AddSpendScreen. */
    fun consumeNavigateToSpendScreen() = _uiState.update { it.copy(navigateToSpendScreen = false) }

    /** Llamar desde AddSpendScreen justo después de procesar spendSaved = true (ya volvió atrás). */
    fun consumeSpendSaved() = _uiState.update { it.copy(spendSaved = false, spendToEdit = null, sharesForEditedSpend = emptyList()) }

    /**
     * Limpia el estado de edición sin tocar spendSaved.
     * Se llama desde onBack en AppNavigation DESPUÉS de popBackStack(),
     * así la transición ya ocurrió y no se produce el parpadeo "Editar → Nuevo".
     */
    fun clearSpendEdit() = _uiState.update { it.copy(spendToEdit = null, sharesForEditedSpend = emptyList()) }

    // Mantener por compatibilidad con AddParticipantInGroup que usa showAddSpendDialog
    fun showAddSpendDialog() = prepareNewSpend()
    fun hideAddSpendDialog() = _uiState.update { it.copy(showAddSpendDialog = false, spendToEdit = null) }
    fun showEditSpendDialog(spend: Spend) = prepareEditSpend(spend)

    fun createEqualSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        participantIds: List<Long>,
        categoryId: Long?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.createEqualSpend(
                    groupId = groupId,
                    concept = concept,
                    amount = amount,
                    payerId = payerId,
                    participantIds = participantIds,
                    categoryId = categoryId,
                    date = Clock.System.now()
                )
                _uiState.update { it.copy(spendSaved = true, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido al crear el gasto") }
            }
        }
    }

    fun createPercentageSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        percentages: Map<Long, Double>,
        categoryId: Long?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.createPercentageSpend(
                    groupId = groupId,
                    concept = concept,
                    amount = amount,
                    payerId = payerId,
                    percentages = percentages,
                    categoryId = categoryId,
                    date = Clock.System.now()
                )
                _uiState.update { it.copy(spendSaved = true, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido al crear el gasto") }
            }
        }
    }

    fun createCustomSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        customAmounts: Map<Long, Double>,
        categoryId: Long?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.createCustomSpend(
                    groupId = groupId,
                    concept = concept,
                    amount = amount,
                    payerId = payerId,
                    customAmounts = customAmounts,
                    categoryId = categoryId,
                    date = Clock.System.now()
                )
                _uiState.update { it.copy(spendSaved = true, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido al crear el gasto") }
            }
        }
    }

    fun deleteSpend(spendId: Long) {
        viewModelScope.launch {
            try {
                deleteMirroredSettlementsForSpendIds(setOf(spendId))
                spendService.deleteSpend(spendId)
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateEqualSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        participantIds: List<Long>,
        categoryId: Long?
    ) {
        val existing = _uiState.value.spendToEdit
        if (existing == null) {
            _uiState.update { it.copy(error = "Error: no se encontró el gasto a editar. Inténtalo de nuevo.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.updateEqualSpend(existing, concept.trim(), amount, payerId, participantIds, categoryId)
                _uiState.update { it.copy(spendSaved = true, spendToEdit = null, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido al guardar el gasto") }
            }
        }
    }

    fun updatePercentageSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        percentages: Map<Long, Double>,
        categoryId: Long?
    ) {
        val existing = _uiState.value.spendToEdit
        if (existing == null) {
            _uiState.update { it.copy(error = "Error: no se encontró el gasto a editar. Inténtalo de nuevo.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.updatePercentageSpend(existing, concept.trim(), amount, payerId, percentages, categoryId)
                _uiState.update { it.copy(spendSaved = true, spendToEdit = null, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido al guardar el gasto") }
            }
        }
    }

    fun updateCustomSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        customAmounts: Map<Long, Double>,
        categoryId: Long?
    ) {
        val existing = _uiState.value.spendToEdit
        if (existing == null) {
            _uiState.update { it.copy(error = "Error: no se encontró el gasto a editar. Inténtalo de nuevo.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.updateCustomSpend(existing, concept.trim(), amount, payerId, customAmounts, categoryId)
                _uiState.update { it.copy(spendSaved = true, spendToEdit = null, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido al guardar el gasto") }
            }
        }
    }

    // --- Liquidaciones ---
    fun showSettleUpDialog() = _uiState.update { it.copy(showSettleUpDialog = true) }
    fun hideSettleUpDialog() = _uiState.update { it.copy(showSettleUpDialog = false) }

    fun createSettlement(fromId: Long, toId: Long, amount: Double, notes: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                settlementService.createSettlement(
                    groupId = groupId,
                    fromParticipantId = fromId,
                    toParticipantId = toId,
                    amount = amount,
                    notes = notes
                )
                _uiState.update { it.copy(showSettleUpDialog = false, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** Liquida varios DebtTransfer de golpe, creando un Settlement por cada uno. */
    fun createSettlementsForTransfers(transfers: List<DebtTransfer>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                transfers.forEach { t ->
                    settlementService.createSettlement(
                        groupId           = groupId,
                        fromParticipantId = t.fromParticipantId,
                        toParticipantId   = t.toParticipantId,
                        amount            = t.amount
                    )
                }
                _uiState.update { it.copy(showSettleUpDialog = false, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // --- Analíticas ---
    fun setAnalyticsSearchQuery(query: String) =
        _uiState.update { it.copy(analyticsSearchQuery = query) }

    fun toggleAnalyticsCategory(categoryId: Long) = _uiState.update { state ->
        val current = state.analyticsSelectedCategories
        state.copy(
            analyticsSelectedCategories =
                if (categoryId in current) current - categoryId else current + categoryId
        )
    }

    fun toggleAnalyticsParticipant(participantId: Long) = _uiState.update { state ->
        val current = state.analyticsSelectedParticipants
        state.copy(
            analyticsSelectedParticipants =
                if (participantId in current) current - participantId else current + participantId
        )
    }

    fun setAnalyticsPeriod(period: AnalyticsPeriod) =
        _uiState.update { it.copy(analyticsPeriod = period) }

    fun clearAnalyticsFilters() = _uiState.update {
        it.copy(
            analyticsSearchQuery = "",
            analyticsSelectedCategories = emptySet(),
            analyticsSelectedParticipants = emptySet(),
            analyticsPeriod = AnalyticsPeriod.Todo
        )
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // --- Ajustes del grupo ---
    fun updateGroup(name: String, description: String, currency: String) {
        val current = _uiState.value.group ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val updated = groupService.updateGroup(
                    current.copy(
                        name = name.trim(),
                        description = description.trim(),
                        currency = currency.trim()
                    )
                )
                _uiState.update { it.copy(group = updated, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun createCategory(name: String, icon: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                categoryService.createCategory(groupId = groupId, name = name.trim(), icon = icon)
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            try {
                categoryService.deleteCategory(categoryId)
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setDefaultSplitPercentages(percentages: Map<Long, Double>) {
        _uiState.update { it.copy(defaultSplitPercentages = percentages) }
    }

    fun deleteSpendsFiltered(
        categoryId: Long? = null,
        payerId: Long? = null,
        beforeInstant: Instant? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val spendIdsToDelete = spendService.getSpends(groupId)
                    .filter { spend ->
                        val catOk = categoryId == null || spend.categoryId == categoryId
                        val payerOk = payerId == null || spend.payerId == payerId
                        val dateOk = beforeInstant == null || spend.date < beforeInstant
                        catOk && payerOk && dateOk
                    }
                    .map { it.id }
                    .toSet()

                deleteMirroredSettlementsForSpendIds(spendIdsToDelete)
                spendService.deleteSpendsFiltered(
                    groupId       = groupId,
                    categoryId    = categoryId,
                    payerId       = payerId,
                    beforeInstant = beforeInstant
                )
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteSpendsByIds(ids: Set<Long>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                deleteMirroredSettlementsForSpendIds(ids)
                spendService.deleteSpendsByIds(ids.toList())
                loadAll()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun deleteMirroredSettlementsForSpendIds(spendIds: Set<Long>) {
        if (spendIds.isEmpty()) return

        val settlementIds = spendService.getSpends(groupId)
            .asSequence()
            .filter { it.id in spendIds }
            .mapNotNull { extractSettlementIdFromNotes(it.notes) }
            .toSet()

        settlementIds.forEach { settlementService.deleteSettlement(it) }
    }

    private fun extractSettlementIdFromNotes(notes: String): Long? {
        if (!notes.startsWith(SETTLEMENT_NOTE_PREFIX)) return null
        return notes
            .removePrefix(SETTLEMENT_NOTE_PREFIX)
            .substringBefore("|")
            .toLongOrNull()
    }
}
