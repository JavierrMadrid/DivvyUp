package com.example.divvyup.integration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvyup.application.ActivityLogService
import com.example.divvyup.application.GroupService
import com.example.divvyup.domain.model.ActivityLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Entrada del feed global — un evento de actividad junto al nombre de su grupo. */
data class ActivityFeedItem(
    val log: ActivityLog,
    val groupName: String
)

data class ActivityFeedUiState(
    val items: List<ActivityFeedItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel del feed de actividad global (pestaña "Actividad" del bottom nav).
 *
 * Agrega los eventos de todos los grupos del usuario ordenados por fecha
 * descendente. Reutiliza [ActivityLogService] (misma fuente que la pestaña
 * Actividad de cada grupo).
 */
class ActivityFeedViewModel(
    private val groupService: GroupService,
    private val activityLogService: ActivityLogService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityFeedUiState())
    val uiState: StateFlow<ActivityFeedUiState> = _uiState.asStateFlow()

    private var loadInFlight = false

    fun load() {
        if (loadInFlight) return
        loadInFlight = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val groups = groupService.getAllGroups()
                val items = groups
                    .flatMap { group ->
                        activityLogService.getActivityLog(group.id)
                            .map { ActivityFeedItem(it, group.name) }
                    }
                    .sortedByDescending { it.log.createdAt }
                _uiState.update { it.copy(items = items, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "No se pudo cargar la actividad")
                }
            } finally {
                loadInFlight = false
            }
        }
    }
}
