package com.example.divvyup.integration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvyup.application.CategoryService
import com.example.divvyup.application.GroupService
import com.example.divvyup.application.SpendService
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.repository.ParticipantRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

data class GroupListUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val createdGroupId: Long? = null,
    // Caché local para el dialog de borrado avanzado
    val participantsByGroup: Map<Long, List<Participant>> = emptyMap(),
    val categoriesByGroup: Map<Long, List<Category>> = emptyMap()
)

class GroupListViewModel(
    private val groupService: GroupService,
    private val spendService: SpendService,
    private val participantRepository: ParticipantRepository,
    private val categoryService: CategoryService,
    private val currentUserIdProvider: (suspend () -> String?) = { null },
    /** Opcional: invalida la caché subyacente antes de recargar (útil tras cambio de sesión). */
    private val cacheInvalidator: (() -> Unit)? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // FASE 1: Cargar y mostrar los grupos inmediatamente — O(1) petición
                val groups = groupService.getAllGroups()
                _uiState.update { it.copy(groups = groups, isLoading = false) }

                // FASE 2: En paralelo y en background — ordenar + precargar datos de detalle.
                // No bloquea la UI; cuando termina actualiza silenciosamente el estado.
                loadGroupDetailsInBackground(groups)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /**
     * Carga en paralelo participantes, categorías y último gasto de cada grupo.
     * Se ejecuta en background (no muestra spinner) para no bloquear la lista inicial.
     * Actualiza el orden y la caché de participantes/categorías cuando termina.
     */
    private fun loadGroupDetailsInBackground(groups: List<Group>) {
        if (groups.isEmpty()) return
        viewModelScope.launch {
            try {
                coroutineScope {
                    // Un async por grupo — todas las peticiones vuelan en paralelo
                    val detailJobs = groups.map { group ->
                        async {
                            val participants = try { participantRepository.getByGroup(group.id) } catch (_: Exception) { emptyList() }
                            val categories   = try { categoryService.getCategories(group.id) } catch (_: Exception) { emptyList() }
                            val lastSpend    = try { spendService.getSpends(group.id).maxOfOrNull { it.date } } catch (_: Exception) { null }
                            Triple(group, participants, categories) to lastSpend
                        }
                    }
                    val results = detailJobs.awaitAll()

                    val participantsMap   = mutableMapOf<Long, List<Participant>>()
                    val categoriesMap     = mutableMapOf<Long, List<Category>>()
                    val groupLastActivity = mutableMapOf<Long, Instant>()

                    results.forEach { (triple, lastSpendInstant) ->
                        val (group, participants, categories) = triple
                        participantsMap[group.id] = participants
                        categoriesMap[group.id]   = categories
                        groupLastActivity[group.id] = if (lastSpendInstant != null && lastSpendInstant > group.createdAt) {
                            lastSpendInstant
                        } else {
                            group.createdAt
                        }
                    }

                    val sortedGroups = groups
                        .sortedWith(compareByDescending<Group> { groupLastActivity[it.id] ?: it.createdAt }
                            .thenByDescending { it.createdAt }
                            .thenByDescending { it.id })

                    _uiState.update {
                        it.copy(
                            groups = sortedGroups,
                            participantsByGroup = participantsMap,
                            categoriesByGroup = categoriesMap
                        )
                    }
                }
            } catch (_: Exception) {
                // Fallo no crítico: la lista ya está visible, solo no se reordena ni precarga
            }
        }
    }

    fun getParticipantsForGroup(groupId: Long): List<Participant> =
        _uiState.value.participantsByGroup[groupId] ?: emptyList()

    fun getCategoriesForGroup(groupId: Long): List<Category> =
        _uiState.value.categoriesByGroup[groupId] ?: emptyList()

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun hideCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }

    fun createGroup(name: String, description: String, currency: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = currentUserIdProvider()
                if (userId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Sin conexión a internet. Conéctate para crear grupos.") }
                    return@launch
                }
                val newGroup = groupService.createGroup(name, description, currency, ownerUserId = userId)
                _uiState.update {
                    it.copy(isLoading = false, showCreateDialog = false, createdGroupId = newGroup.id)
                }
                loadGroups()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun consumeNavigation() = _uiState.update { it.copy(createdGroupId = null) }

    /**
     * Invalida la caché de grupos (si existe) y recarga desde Supabase.
     * Llamar tras un cambio de autenticación (login/logout) para que el nuevo
     * usuario vea sus grupos en lugar de los de la sesión anterior.
     * Los grupos se limpian de inmediato para que no queden visibles los del usuario anterior.
     */
    fun reloadAfterAuthChange() {
        cacheInvalidator?.invoke()
        _uiState.update { it.copy(groups = emptyList(), participantsByGroup = emptyMap(), categoriesByGroup = emptyMap()) }
        loadGroups()
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                groupService.deleteGroup(id)
                loadGroups()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteSpendsForGroup(
        groupId: Long,
        categoryId: Long?,
        payerId: Long?,
        beforeInstant: Instant?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.deleteSpendsFiltered(
                    groupId       = groupId,
                    categoryId    = categoryId,
                    payerId       = payerId,
                    beforeInstant = beforeInstant
                )
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteSpendsByIds(ids: Set<Long>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.deleteSpendsByIds(ids.toList())
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
