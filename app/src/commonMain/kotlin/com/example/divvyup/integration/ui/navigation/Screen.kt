package com.example.divvyup.integration.ui.navigation

import kotlinx.serialization.Serializable

// Rutas de navegación como clases @Serializable (type-safe navigation KMP)
sealed interface Screen {
    @Serializable data object GroupList : Screen
    @Serializable data object CreateGroup : Screen
    @Serializable data class AddParticipants(val groupId: Long) : Screen
    @Serializable data class GroupDetail(val groupId: Long) : Screen
    @Serializable data class AddSpend(val groupId: Long) : Screen
    @Serializable data class AddParticipantInGroup(val groupId: Long) : Screen
    @Serializable data class GroupSettings(val groupId: Long) : Screen
}
