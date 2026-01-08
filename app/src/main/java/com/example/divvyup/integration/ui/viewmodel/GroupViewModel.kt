package com.example.divvyup.integration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvyup.application.GroupService
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupListUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class GroupDetailUiState(
    val group: Group? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class GroupViewModel(private val groupService: GroupService) : ViewModel() {

    private val _groupListState = MutableStateFlow(GroupListUiState())
    val groupListState: StateFlow<GroupListUiState> = _groupListState.asStateFlow()

    private val _groupDetailState = MutableStateFlow(GroupDetailUiState())
    val groupDetailState: StateFlow<GroupDetailUiState> = _groupDetailState.asStateFlow()

    init {
        loadGroups()
    }

    fun loadGroups() {
        viewModelScope.launch {
            _groupListState.value = _groupListState.value.copy(isLoading = true, error = null)
            try {
                val groups = groupService.getGroups()
                _groupListState.value = _groupListState.value.copy(
                    groups = groups,
                    isLoading = false
                )
            } catch (e: Exception) {
                _groupListState.value = _groupListState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            _groupDetailState.value = _groupDetailState.value.copy(isLoading = true, error = null)
            try {
                val group = groupService.getGroup(groupId)
                _groupDetailState.value = _groupDetailState.value.copy(
                    group = group,
                    isLoading = false
                )
            } catch (e: Exception) {
                _groupDetailState.value = _groupDetailState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }

    fun createGroup(name: String) {
        viewModelScope.launch {
            try {
                val newGroup = Group(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name
                )
                groupService.createOrUpdateGroup(newGroup)
                loadGroups()
            } catch (e: Exception) {
                _groupListState.value = _groupListState.value.copy(
                    error = e.message ?: "Error al crear grupo"
                )
            }
        }
    }

    fun addUserToGroup(groupId: String, user: User) {
        viewModelScope.launch {
            try {
                groupService.addUserToGroup(groupId, user)
                loadGroup(groupId)
            } catch (e: Exception) {
                _groupDetailState.value = _groupDetailState.value.copy(
                    error = e.message ?: "Error al añadir usuario"
                )
            }
        }
    }

    fun removeUserFromGroup(groupId: String, user: User) {
        viewModelScope.launch {
            try {
                groupService.removeUserFromGroup(groupId, user)
                loadGroup(groupId)
            } catch (e: Exception) {
                _groupDetailState.value = _groupDetailState.value.copy(
                    error = e.message ?: "Error al eliminar usuario"
                )
            }
        }
    }

    fun addSpend(groupId: String, spend: Spend) {
        viewModelScope.launch {
            try {
                groupService.addOrUpdateSpend(groupId, spend)
                loadGroup(groupId)
            } catch (e: Exception) {
                _groupDetailState.value = _groupDetailState.value.copy(
                    error = e.message ?: "Error al añadir gasto"
                )
            }
        }
    }

    fun removeSpend(groupId: String, spend: Spend) {
        viewModelScope.launch {
            try {
                groupService.removeSpend(groupId, spend)
                loadGroup(groupId)
            } catch (e: Exception) {
                _groupDetailState.value = _groupDetailState.value.copy(
                    error = e.message ?: "Error al eliminar gasto"
                )
            }
        }
    }
}

