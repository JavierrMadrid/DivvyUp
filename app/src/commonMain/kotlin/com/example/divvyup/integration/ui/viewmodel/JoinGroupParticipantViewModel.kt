package com.example.divvyup.integration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvyup.application.InvitationService
import com.example.divvyup.domain.model.Participant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JoinGroupParticipantUiState(
    val groupId: Long = 0,
    val groupName: String = "",
    val participants: List<Participant> = emptyList(),
    val selectedParticipantId: Long? = null,
    val alreadyAssignedParticipantId: Long? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val navigateToGroupDetail: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel para la pantalla de unión a grupo por invitación.
 *
 * @param inviteToken  Token del deep link `divvyup://join?token=<uuid>`. Si es null se
 *                     usa [groupId] directamente (acceso sin token, p.ej. propietario).
 */
class JoinGroupParticipantViewModel(
    private val inviteToken: String?,
    private val groupIdFallback: Long,
    private val invitationService: InvitationService,
    private val currentUserIdProvider: suspend () -> String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        JoinGroupParticipantUiState(groupId = groupIdFallback, isLoading = true)
    )
    val uiState: StateFlow<JoinGroupParticipantUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = currentUserIdProvider()
                    ?: throw Exception("Debes iniciar sesión para unirte al grupo")

                val joinInfo = if (inviteToken != null) {
                    invitationService.getJoinInfoByToken(inviteToken)
                } else {
                    invitationService.getJoinInfo(groupIdFallback)
                }

                val assignedParticipantId =
                    invitationService.getAssignedParticipantId(joinInfo.group.id, userId)

                _uiState.update {
                    it.copy(
                        groupId = joinInfo.group.id,
                        groupName = joinInfo.group.name,
                        participants = joinInfo.participants,
                        selectedParticipantId = assignedParticipantId,
                        alreadyAssignedParticipantId = assignedParticipantId,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "No se pudo cargar la invitación") }
            }
        }
    }

    fun selectParticipant(participantId: Long) {
        _uiState.update { it.copy(selectedParticipantId = participantId, error = null) }
    }

    fun confirmSelection() {
        val participantId = _uiState.value.selectedParticipantId
        if (participantId == null) {
            _uiState.update { it.copy(error = "Selecciona tu participante para continuar") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val userId = currentUserIdProvider()
                    ?: throw Exception("Debes iniciar sesión para unirte al grupo")
                invitationService.assignUserToParticipant(
                    groupId = _uiState.value.groupId,
                    participantId = participantId,
                    userId = userId
                )
                _uiState.update { it.copy(isSaving = false, navigateToGroupDetail = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message ?: "No se pudo asignar el participante") }
            }
        }
    }

    fun consumeNavigateToGroupDetail() {
        _uiState.update { it.copy(navigateToGroupDetail = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
