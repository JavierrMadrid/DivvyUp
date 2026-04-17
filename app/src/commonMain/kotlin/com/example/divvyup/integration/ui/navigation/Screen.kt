package com.example.divvyup.integration.ui.navigation

import kotlinx.serialization.Serializable

// Rutas de navegación como clases @Serializable (type-safe navigation KMP)
sealed interface Screen {
    @Serializable data class Login(val confirmationMessage: String? = null) : Screen
    @Serializable data object Register : Screen
    @Serializable data object GroupList : Screen
    @Serializable data object CreateGroup : Screen
    @Serializable data class AddParticipants(val groupId: Long) : Screen
    @Serializable data class GroupDetail(val groupId: Long) : Screen
    @Serializable data class SettleUp(val groupId: Long) : Screen
    @Serializable data class AddSpend(val groupId: Long) : Screen
    @Serializable data class AddParticipantInGroup(val groupId: Long) : Screen
    @Serializable data class GroupSettings(val groupId: Long) : Screen
    /** [inviteToken] viene del deep link `divvyup://join?token=<uuid>`.
     *  [groupId] = 0 cuando se llega por token; se usa como fallback para acceso directo. */
    @Serializable data class JoinGroupParticipant(
        val inviteToken: String?,
        val groupId: Long = 0
    ) : Screen
    @Serializable data object UserSettings : Screen
    @Serializable data object ChangePassword : Screen
}
