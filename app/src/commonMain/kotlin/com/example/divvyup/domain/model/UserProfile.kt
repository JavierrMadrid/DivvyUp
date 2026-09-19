package com.example.divvyup.domain.model

/** Perfil público de un usuario autenticado. */
data class UserProfile(
    val userId: String,
    val displayName: String = "",
    val avatarUrl: String? = null
)

