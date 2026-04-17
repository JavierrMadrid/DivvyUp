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
                val groups = groupService.getAllGroups()
                val groupLastActivity = mutableMapOf<Long, Instant>()
                // Precarga participantes y categorías para el dialog de borrado avanzado
                val participantsMap = mutableMapOf<Long, List<Participant>>()
                val categoriesMap   = mutableMapOf<Long, List<Category>>()
                groups.forEach { group ->
                    participantsMap[group.id] = participantRepository.getByGroup(group.id)
                    categoriesMap[group.id]   = categoryService.getCategories(group.id)
                    // "Modificar grupo" = última fecha de gasto o fecha de creación del grupo.
                    val lastSpendInstant = spendService.getSpends(group.id).maxOfOrNull { it.date }
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
                        categoriesByGroup = categoriesMap,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
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
     */
    fun reloadAfterAuthChange() {
        cacheInvalidator?.invoke()
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



