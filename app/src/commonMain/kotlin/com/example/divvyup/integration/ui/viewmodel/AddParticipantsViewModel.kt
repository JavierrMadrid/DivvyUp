package com.example.divvyup.integration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.repository.ParticipantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParticipantDraft(
    val name: String = "",
    val email: String = "",
    val nameError: Boolean = false
)

data class AddParticipantsUiState(
    val groupId: Long = 0L,
    val participants: List<Participant> = emptyList(),
    val draft: ParticipantDraft = ParticipantDraft(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val navigateToDetail: Boolean = false
)

class AddParticipantsViewModel(
    private val groupId: Long,
    private val participantRepository: ParticipantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddParticipantsUiState(groupId = groupId))
    val uiState: StateFlow<AddParticipantsUiState> = _uiState.asStateFlow()

    // --- Draft editing ---
    fun updateDraftName(name: String) =
        _uiState.update { it.copy(draft = it.draft.copy(name = name, nameError = false)) }

    fun updateDraftEmail(email: String) =
        _uiState.update { it.copy(draft = it.draft.copy(email = email)) }

    fun addParticipant() {
        val draft = _uiState.value.draft
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(draft = it.draft.copy(nameError = true)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val saved = participantRepository.create(
                    Participant(
                        groupId = groupId,
                        name = draft.name.trim(),
                        email = draft.email.trim().takeIf { it.isNotBlank() }
                    )
                )
                _uiState.update {
                    it.copy(
                        participants = it.participants + saved,
                        draft = ParticipantDraft(),
                        isSaving = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun removeParticipant(participant: Participant) {
        viewModelScope.launch {
            try {
                if (participant.id != 0L) {
                    participantRepository.delete(participant.id)
                }
                _uiState.update { it.copy(participants = it.participants - participant) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun finishAndNavigate() {
        _uiState.update { it.copy(navigateToDetail = true) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun consumeNavigation() = _uiState.update { it.copy(navigateToDetail = false) }
}

