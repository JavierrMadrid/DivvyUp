package com.example.divvyup.domain.model

import kotlin.time.Instant

// Dominio puro — sin anotaciones de framework (skill: android-clean-architecture)
data class Group(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val currency: String = "EUR",
    val ownerUserId: String? = null,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0)
)

