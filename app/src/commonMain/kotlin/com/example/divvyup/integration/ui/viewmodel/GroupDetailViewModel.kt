@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.example.divvyup.integration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvyup.application.ActivityLogService
import com.example.divvyup.application.CategoryService
import com.example.divvyup.application.GroupService
import com.example.divvyup.application.SETTLEMENT_NOTE_PREFIX
import com.example.divvyup.application.SettlementService
import com.example.divvyup.application.SpendService
import com.example.divvyup.domain.model.ActivityEventType
import com.example.divvyup.domain.model.ActivityLog
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.DebtTransfer
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Recurrence
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.domain.repository.ParticipantRepository
import com.example.divvyup.domain.repository.ParticipantUserLinkRepository
import com.example.divvyup.domain.repository.UserProfileRepository
import com.example.divvyup.integration.supabase.SupabaseStorageService
import com.example.divvyup.integration.ui.resolveDefaultSplitPercentages
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.time.Clock
import kotlin.time.Instant

// Filtro de período para analíticas
sealed class AnalyticsPeriod {
    data object Todo : AnalyticsPeriod()
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
    val spendToEdit: Spend? = null,
    val sharesForEditedSpend: List<SpendShare> = emptyList(),
    // Señales de navegación
    val navigateToSpendScreen: Boolean = false,
    val spendSaved: Boolean = false,
    // Analíticas — filtros
    val analyticsSearchQuery: String = "",
    val analyticsSelectedCategories: Set<Long> = emptySet(),
    val analyticsSelectedParticipants: Set<Long> = emptySet(),
    val analyticsPeriod: AnalyticsPeriod = AnalyticsPeriod.Todo,
    // Ajustes del grupo
    val defaultSplitPercentages: Map<Long, Double> = emptyMap(),
    val settingsSavedMessage: String? = null,
    /** true si el usuario con sesión activa es el propietario del grupo. */
    val isOwner: Boolean = false,
    // Impacto personal por gasto — participante vinculado al usuario actual
    /** ID del participante vinculado al usuario que tiene la sesión abierta. Null si no está vinculado. */
    val myParticipantId: Long? = null,
    /** spendId → impacto neto (positivo = le deben, negativo = debe). Vacío si no hay participante vinculado. */
    val spendPersonalImpact: Map<Long, Double> = emptyMap(),
    // Exportación — texto listo para compartir (null = sin pendiente)
    val pendingExportText: String? = null,
    /** Datos listos para generar PDF en la plataforma (null = sin pendiente). */
    val pendingExportPdf: com.example.divvyup.application.AnalyticsExportData? = null,
    /** Datos listos para generar Excel en la plataforma (null = sin pendiente). */
    val pendingExportExcel: com.example.divvyup.application.AnalyticsExportData? = null,
    /** participantId → URL del avatar (solo para participantes vinculados con perfil). */
    val participantAvatarUrls: Map<Long, String> = emptyMap(),
    // Historial de actividad
    val activityLog: List<ActivityLog> = emptyList()
)

enum class GroupDetailTab { GASTOS, BALANCES, ANALITICAS, ACTIVIDAD }

class GroupDetailViewModel(
    private val groupId: Long,
    private val groupService: GroupService,
    private val spendService: SpendService,
    private val settlementService: SettlementService,
    private val categoryService: CategoryService,
    private val participantRepository: ParticipantRepository,
    /** Proveedor del ID de participante vinculado al usuario actual (puede ser null). */
    private val myParticipantIdProvider: (suspend () -> Long?) = { null },
    /** Proveedor del ID de usuario actual para permitir cambiar el participante vinculado. */
    private val currentUserIdProvider: (suspend () -> String?) = { null },
    private val participantUserLinkRepository: ParticipantUserLinkRepository? = null,
    private val userProfileRepository: UserProfileRepository? = null,
    private val activityLogService: ActivityLogService? = null,
    private val storageService: SupabaseStorageService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Lanzar todas las peticiones en paralelo para reducir tiempo de carga inicial
                coroutineScope {
                    val groupD        = async { groupService.getGroup(groupId) }
                    val participantsD = async { participantRepository.getByGroup(groupId) }
                    val spendsD       = async { spendService.getSpends(groupId) }
                    val categoriesD   = async { categoryService.getCategories(groupId) }
                    val balancesD     = async { settlementService.getBalances(groupId) }
                    val settlementsD  = async { settlementService.getSettlements(groupId) }
                    val myPartIdD     = async { myParticipantIdProvider() }
                    val currentUserD  = async { currentUserIdProvider() }
                    val avatarsD      = async {
                        try { userProfileRepository?.getAvatarUrlsForGroup(groupId) ?: emptyMap() }
                        catch (_: Exception) { emptyMap<Long, String>() }
                    }
                    val activityD     = async {
                        try { activityLogService?.getActivityLog(groupId) ?: emptyList() }
                        catch (_: Exception) { emptyList<ActivityLog>() }
                    }

                    val group         = groupD.await()
                    val participants  = participantsD.await()
                    val spends        = spendsD.await()
                    val categories    = categoriesD.await()
                    val balances      = balancesD.await()
                    val settlements   = settlementsD.await()
                    val myParticipantId = myPartIdD.await()
                    val currentUserId = currentUserD.await()
                    val avatarUrls    = avatarsD.await()
                    val activityLog   = activityD.await()

                    val transfers = settlementService.simplifyDebts(balances)
                    val isOwner = group.ownerUserId == null ||
                        (currentUserId != null && group.ownerUserId == currentUserId)
                    val personalImpact = if (myParticipantId != null) {
                        spendService.getPersonalImpactByGroup(groupId, myParticipantId)
                    } else emptyMap()

                    _uiState.update {
                        it.copy(
                            group = group,
                            participants = participants,
                            spends = spends,
                            categories = categories,
                            balances = balances,
                            debtTransfers = transfers,
                            settlements = settlements,
                            defaultSplitPercentages = resolveDefaultSplitPercentages(
                                participantIds = participants.map { p -> p.id },
                                savedPercentages = it.defaultSplitPercentages
                            ),
                            myParticipantId = myParticipantId,
                            isOwner = isOwner,
                            activityLog = activityLog,
                            spendPersonalImpact = personalImpact,
                            participantAvatarUrls = avatarUrls,
                            isLoading = false
                        )
                    }
                    // Si no se pudo determinar la propiedad (sesión no lista aún), reintenta
                    if (currentUserId == null && group.ownerUserId != null) {
                        scheduleOwnershipRecheck(group.ownerUserId)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Reintenta obtener el userId y actualiza isOwner cuando la sesión ya está disponible.
     * Se lanza solo cuando currentUserIdProvider() devolvió null durante loadAll().
     */
    private fun scheduleOwnershipRecheck(ownerUserId: String) {
        viewModelScope.launch {
            repeat(5) { attempt ->
                kotlinx.coroutines.delay(300L * (attempt + 1))
                val uid = currentUserIdProvider()
                if (uid != null) {
                    _uiState.update { it.copy(isOwner = ownerUserId == uid) }
                    return@launch
                }
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

    /** Cambia el participante vinculado al usuario actual en este grupo. */
    fun selectMyParticipant(participantId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = currentUserIdProvider()
                    ?: throw Exception("No hay sesión activa")
                val linkRepo = participantUserLinkRepository
                    ?: throw Exception("Repositorio de vínculos no disponible")

                // Primero eliminar el vínculo actual del usuario en este grupo (si existe)
                try {
                    linkRepo.removeUserLink(groupId, userId)
                } catch (_: Exception) {
                }

                // Crear el nuevo vínculo
                linkRepo.assignUserToParticipant(groupId, participantId, userId)

                // Recalcular impacto personal con el nuevo participante
                val personalImpact = spendService.getPersonalImpactByGroup(groupId, participantId)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        myParticipantId = participantId,
                        spendPersonalImpact = personalImpact,
                        settingsSavedMessage = "Participante actualizado"
                    )
                }
            } catch (e: Exception) {
                println("DEBUG GroupDetailViewModel: selectMyParticipant error — ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No se pudo cambiar el participante: ${e.message}"
                    )
                }
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
        if (spend.notes.startsWith(SETTLEMENT_NOTE_PREFIX)) {
            _uiState.update { it.copy(error = "Las liquidaciones espejo no se pueden editar. Puedes borrarlas desde la lista.") }
            return
        }
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
    fun consumeSpendSaved() = _uiState.update {
        it.copy(
            spendSaved = false,
            spendToEdit = null,
            sharesForEditedSpend = emptyList()
        )
    }

    /**
     * Limpia el estado de edición sin tocar spendSaved.
     * Se llama desde onBack en AppNavigation DESPUÉS de popBackStack(),
     * así la transición ya ocurrió y no se produce el parpadeo "Editar → Nuevo".
     */
    fun clearSpendEdit() =
        _uiState.update { it.copy(spendToEdit = null, sharesForEditedSpend = emptyList()) }

    // Mantener por compatibilidad con AddParticipantInGroup que usa showAddSpendDialog
    fun showAddSpendDialog() = prepareNewSpend()
    fun hideAddSpendDialog() =
        _uiState.update { it.copy(showAddSpendDialog = false, spendToEdit = null) }

    fun showEditSpendDialog(spend: Spend) = prepareEditSpend(spend)

    fun createEqualSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        participantIds: List<Long>,
        categoryId: Long?,
        recurrence: Recurrence = Recurrence.NONE,
        receiptUrl: String? = null,
        date: Instant = Clock.System.now()
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
                    date = date,
                    recurrence = recurrence,
                    receiptUrl = receiptUrl
                )
                val payerName = _uiState.value.participants.firstOrNull { it.id == payerId }?.name
                activityLogService?.logEvent(groupId, ActivityEventType.GASTO_CREADO, "Gasto «$concept» añadido (${"%.2f".format(amount)})", actorName = payerName)
                _uiState.update { it.copy(spendSaved = true, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido al crear el gasto"
                    )
                }
            }
        }
    }

    fun createPercentageSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        percentages: Map<Long, Double>,
        categoryId: Long?,
        recurrence: Recurrence = Recurrence.NONE,
        receiptUrl: String? = null,
        date: Instant = Clock.System.now()
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
                    date = date,
                    recurrence = recurrence,
                    receiptUrl = receiptUrl
                )
                val payerName = _uiState.value.participants.firstOrNull { it.id == payerId }?.name
                activityLogService?.logEvent(groupId, ActivityEventType.GASTO_CREADO, "Gasto «$concept» añadido (${"%.2f".format(amount)})", actorName = payerName)
                _uiState.update { it.copy(spendSaved = true, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido al crear el gasto"
                    )
                }
            }
        }
    }

    fun createCustomSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        customAmounts: Map<Long, Double>,
        categoryId: Long?,
        recurrence: Recurrence = Recurrence.NONE,
        receiptUrl: String? = null,
        date: Instant = Clock.System.now()
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
                    date = date,
                    recurrence = recurrence,
                    receiptUrl = receiptUrl
                )
                val payerName = _uiState.value.participants.firstOrNull { it.id == payerId }?.name
                activityLogService?.logEvent(groupId, ActivityEventType.GASTO_CREADO, "Gasto «$concept» añadido (${"%.2f".format(amount)})", actorName = payerName)
                _uiState.update { it.copy(spendSaved = true, isLoading = false) }
                loadAll()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido al crear el gasto"
                    )
                }
            }
        }
    }

    fun deleteSpend(spendId: Long) {
        viewModelScope.launch {
            try {
                val spend = _uiState.value.spends.firstOrNull { it.id == spendId }
                deleteMirroredSettlementsForSpendIds(setOf(spendId))
                spendService.deleteSpend(spendId)
                if (spend != null && spend.notes.startsWith(SETTLEMENT_NOTE_PREFIX)) {
                    val participants = _uiState.value.participants
                    val fromName = participants.firstOrNull { it.id == spend.payerId }?.name ?: "?"
                    activityLogService?.logEvent(
                        groupId, ActivityEventType.LIQUIDACION_ELIMINADA,
                        "Liquidación de $fromName eliminada (${"%.2f".format(spend.amount)})"
                    )
                } else {
                    activityLogService?.logEvent(
                        groupId, ActivityEventType.GASTO_ELIMINADO,
                        "Gasto «${spend?.concept ?: "gasto"}» eliminado"
                    )
                }
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
        categoryId: Long?,
        recurrence: Recurrence = Recurrence.NONE,
        receiptUrl: String? = null,
        date: Instant? = null
    ) {
        val existing = _uiState.value.spendToEdit
        if (existing == null) {
            _uiState.update { it.copy(error = "Error: no se encontró el gasto a editar. Inténtalo de nuevo.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.updateEqualSpend(
                    existing, concept.trim(), amount, payerId, participantIds, categoryId,
                    date = date ?: existing.date,
                    recurrence = recurrence, receiptUrl = receiptUrl
                )
                val desc = buildSpendEditDescription(existing, concept.trim(), amount, payerId, categoryId, existing.splitType, recurrence)
                activityLogService?.logEvent(groupId, ActivityEventType.GASTO_EDITADO, desc)
                _uiState.update {
                    it.copy(
                        spendSaved = true,
                        spendToEdit = null,
                        isLoading = false
                    )
                }
                loadAll()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido al guardar el gasto"
                    )
                }
            }
        }
    }

    fun updatePercentageSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        percentages: Map<Long, Double>,
        categoryId: Long?,
        recurrence: Recurrence = Recurrence.NONE,
        receiptUrl: String? = null,
        date: Instant? = null
    ) {
        val existing = _uiState.value.spendToEdit
        if (existing == null) {
            _uiState.update { it.copy(error = "Error: no se encontró el gasto a editar. Inténtalo de nuevo.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.updatePercentageSpend(
                    existing, concept.trim(), amount, payerId, percentages, categoryId,
                    date = date ?: existing.date,
                    recurrence = recurrence, receiptUrl = receiptUrl
                )
                val desc = buildSpendEditDescription(existing, concept.trim(), amount, payerId, categoryId, existing.splitType, recurrence)
                activityLogService?.logEvent(groupId, ActivityEventType.GASTO_EDITADO, desc)
                _uiState.update {
                    it.copy(
                        spendSaved = true,
                        spendToEdit = null,
                        isLoading = false
                    )
                }
                loadAll()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido al guardar el gasto"
                    )
                }
            }
        }
    }

    fun updateCustomSpend(
        concept: String,
        amount: Double,
        payerId: Long,
        customAmounts: Map<Long, Double>,
        categoryId: Long?,
        recurrence: Recurrence = Recurrence.NONE,
        receiptUrl: String? = null,
        date: Instant? = null
    ) {
        val existing = _uiState.value.spendToEdit
        if (existing == null) {
            _uiState.update { it.copy(error = "Error: no se encontró el gasto a editar. Inténtalo de nuevo.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.updateCustomSpend(
                    existing, concept.trim(), amount, payerId, customAmounts, categoryId,
                    date = date ?: existing.date,
                    recurrence = recurrence, receiptUrl = receiptUrl
                )
                val desc = buildSpendEditDescription(existing, concept.trim(), amount, payerId, categoryId, existing.splitType, recurrence)
                activityLogService?.logEvent(groupId, ActivityEventType.GASTO_EDITADO, desc)
                _uiState.update {
                    it.copy(
                        spendSaved = true,
                        spendToEdit = null,
                        isLoading = false
                    )
                }
                loadAll()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error desconocido al guardar el gasto"
                    )
                }
            }
        }
    }

    // --- Liquidaciones ---
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
                val fromName = _uiState.value.participants.firstOrNull { it.id == fromId }?.name ?: "?"
                val toName = _uiState.value.participants.firstOrNull { it.id == toId }?.name ?: "?"
                activityLogService?.logEvent(groupId, ActivityEventType.LIQUIDACION_CREADA, "Liquidación de $fromName a $toName (${"%.2f".format(amount)})")
                _uiState.update { it.copy(isLoading = false) }
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
                val participants = _uiState.value.participants
                transfers.forEach { t ->
                    settlementService.createSettlement(
                        groupId = groupId,
                        fromParticipantId = t.fromParticipantId,
                        toParticipantId = t.toParticipantId,
                        amount = t.amount
                    )
                    val fromName = participants.firstOrNull { it.id == t.fromParticipantId }?.name ?: "?"
                    val toName   = participants.firstOrNull { it.id == t.toParticipantId }?.name ?: "?"
                    activityLogService?.logEvent(
                        groupId, ActivityEventType.LIQUIDACION_CREADA,
                        "Liquidación de $fromName a $toName (${"%.2f".format(t.amount)})"
                    )
                }
                _uiState.update { it.copy(isLoading = false) }
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
    fun consumeSettingsSavedMessage() = _uiState.update { it.copy(settingsSavedMessage = null) }

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

    fun saveGroupSettings(
        name: String,
        description: String,
        currency: String,
        defaultSplitPercentages: Map<Long, Double>,
        defaultCategoryId: Long? = null
    ) {
        val current = _uiState.value.group ?: return
        val participantIds = _uiState.value.participants.map { it.id }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, settingsSavedMessage = null) }
            try {
                val updated = groupService.updateGroup(
                    current.copy(
                        name = name.trim(),
                        description = description.trim(),
                        currency = currency.trim(),
                        defaultCategoryId = defaultCategoryId
                    )
                )
                _uiState.update {
                    it.copy(
                        group = updated,
                        defaultSplitPercentages = resolveDefaultSplitPercentages(
                            participantIds = participantIds,
                            savedPercentages = defaultSplitPercentages
                        ),
                        isLoading = false,
                        settingsSavedMessage = "Cambios guardados correctamente"
                    )
                }
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

    fun updateCategoryBudget(categoryId: Long, budget: Double?) {
        viewModelScope.launch {
            try {
                val cat =
                    _uiState.value.categories.firstOrNull { it.id == categoryId } ?: return@launch
                val updated = categoryService.updateCategoryBudget(cat, budget)
                _uiState.update { state ->
                    state.copy(categories = state.categories.map { if (it.id == categoryId) updated else it })
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setDefaultSplitPercentages(percentages: Map<Long, Double>) {
        val participantIds = _uiState.value.participants.map { it.id }
        _uiState.update {
            it.copy(
                defaultSplitPercentages = resolveDefaultSplitPercentages(
                    participantIds = participantIds,
                    savedPercentages = percentages
                )
            )
        }
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
                    groupId = groupId,
                    categoryId = categoryId,
                    payerId = payerId,
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
                // Log de actividad: un evento por gasto eliminado
                val spendsInState = _uiState.value.spends
                ids.forEach { spendId ->
                    val spend = spendsInState.firstOrNull { it.id == spendId }
                    if (spend != null) {
                        activityLogService?.logEvent(
                            groupId, ActivityEventType.GASTO_ELIMINADO,
                            "Gasto «${spend.concept}» eliminado"
                        )
                    }
                }
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

        settlementIds.forEach { settlementService.deleteSettlement(groupId = groupId, id = it) }
    }

    private fun extractSettlementIdFromNotes(notes: String): Long? {
        if (!notes.startsWith(SETTLEMENT_NOTE_PREFIX)) return null
        return notes
            .removePrefix(SETTLEMENT_NOTE_PREFIX)
            .substringBefore("|")
            .toLongOrNull()
    }

    // ── Exportación ──────────────────────────────────────────────────────────

    fun exportGroupText(filteredSpends: List<Spend>) {
        val state = _uiState.value
        val group = state.group ?: return
        val text = com.example.divvyup.application.GroupExportService.buildTextSummary(
            group = group,
            participants = state.participants,
            spends = filteredSpends,
            categories = state.categories,
            balances = state.balances,
            debtTransfers = state.debtTransfers
        )
        _uiState.update { it.copy(pendingExportText = text) }
    }

    fun exportGroupCsv(filteredSpends: List<Spend>) {
        val state = _uiState.value
        val group = state.group ?: return
        val csv = com.example.divvyup.application.GroupExportService.buildCsv(
            group = group,
            participants = state.participants,
            spends = filteredSpends,
            categories = state.categories,
            balances = state.balances
        )
        _uiState.update { it.copy(pendingExportText = csv) }
    }

    fun consumeExportText() = _uiState.update { it.copy(pendingExportText = null) }
    fun consumeExportPdf()  = _uiState.update { it.copy(pendingExportPdf = null) }
    fun consumeExportExcel() = _uiState.update { it.copy(pendingExportExcel = null) }

    private fun buildExportData(filteredSpends: List<com.example.divvyup.domain.model.Spend>, periodLabel: String): com.example.divvyup.application.AnalyticsExportData? {
        val state = _uiState.value
        val group = state.group ?: return null
        return com.example.divvyup.application.AnalyticsExportData(
            group        = group,
            participants = state.participants,
            spends       = filteredSpends,
            categories   = state.categories,
            balances     = state.balances,
            debtTransfers = state.debtTransfers,
            settlements  = state.settlements,
            periodLabel  = periodLabel
        )
    }

    fun exportGroupPdf(filteredSpends: List<com.example.divvyup.domain.model.Spend>, periodLabel: String = "Todos los periodos") {
        val data = buildExportData(filteredSpends, periodLabel) ?: return
        _uiState.update { it.copy(pendingExportPdf = data) }
    }

    fun exportGroupExcel(filteredSpends: List<com.example.divvyup.domain.model.Spend>, periodLabel: String = "Todos los periodos") {
        val data = buildExportData(filteredSpends, periodLabel) ?: return
        _uiState.update { it.copy(pendingExportExcel = data) }
    }

    /**
     * Sube los bytes de una imagen al bucket "receipts" de Supabase Storage
     * y devuelve la URL pública, o null si falla o no hay servicio configurado.
     */
    suspend fun uploadReceiptImage(imageBytes: ByteArray): String? {
        return try {
            storageService?.uploadReceiptImage(groupId, imageBytes)
        } catch (e: Exception) {
            println("DEBUG GroupDetailViewModel: Error al subir imagen: ${e.message}")
            null
        }
    }

    /**
     * Sube los bytes de una imagen como foto de portada del grupo y actualiza el grupo.
     */
    fun uploadGroupPhotoAndSave(imageBytes: ByteArray) {
        val current = _uiState.value.group ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val url = storageService?.uploadGroupPhoto(groupId, imageBytes)
                    ?: throw Exception("Servicio de almacenamiento no disponible")
                val updated = groupService.updateGroup(current.copy(photoUrl = url))
                _uiState.update {
                    it.copy(
                        group = updated,
                        isLoading = false,
                        settingsSavedMessage = "Foto del grupo actualizada"
                    )
                }
            } catch (e: Exception) {
                println("DEBUG GroupDetailViewModel: uploadGroupPhotoAndSave error — ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "No se pudo subir la foto: ${e.message}"
                    )
                }
            }
        }
    }

    // ── Diff de edición de gasto para el historial de actividad ───────────────
    private fun buildSpendEditDescription(
        existing: Spend,
        newConcept: String,
        newAmount: Double,
        newPayerId: Long,
        newCategoryId: Long?,
        newSplitType: SplitType,
        newRecurrence: Recurrence
    ): String {
        val participants = _uiState.value.participants
        val categories   = _uiState.value.categories
        val currency     = _uiState.value.group?.currency ?: ""

        fun payerName(id: Long) = participants.firstOrNull { it.id == id }?.name ?: "?"
        fun catName(id: Long?)  = id?.let { categories.firstOrNull { c -> c.id == it }?.let { c -> "${c.icon} ${c.name}" } } ?: "Sin categoría"
        fun splitLabel(s: SplitType) = when (s) {
            SplitType.EQUAL      -> "Equitativo"
            SplitType.PERCENTAGE -> "Porcentaje"
            SplitType.CUSTOM     -> "Exacto"
        }
        fun recurrenceLabel(r: Recurrence) = when (r) {
            Recurrence.NONE    -> "Una vez"
            Recurrence.DAILY   -> "Diaria"
            Recurrence.WEEKLY  -> "Semanal"
            Recurrence.MONTHLY -> "Mensual"
        }

        val changes = mutableListOf<String>()

        if (existing.concept.trim() != newConcept)
            changes += "Concepto: «${existing.concept}» → «$newConcept»"
        if (kotlin.math.abs(existing.amount - newAmount) >= 0.005)
            changes += "Importe: ${"%.2f".format(existing.amount)} → ${"%.2f".format(newAmount)} $currency"
        if (existing.payerId != newPayerId)
            changes += "Pagador: ${payerName(existing.payerId)} → ${payerName(newPayerId)}"
        if (existing.categoryId != newCategoryId)
            changes += "Categoría: ${catName(existing.categoryId)} → ${catName(newCategoryId)}"
        if (existing.splitType != newSplitType)
            changes += "Reparto: ${splitLabel(existing.splitType)} → ${splitLabel(newSplitType)}"
        if (existing.recurrence != newRecurrence)
            changes += "Repetición: ${recurrenceLabel(existing.recurrence)} → ${recurrenceLabel(newRecurrence)}"

        val header = "Gasto «$newConcept» editado"
        return if (changes.isEmpty()) header
        else header + "\n" + changes.joinToString("\n") { "• $it" }
    }
}
