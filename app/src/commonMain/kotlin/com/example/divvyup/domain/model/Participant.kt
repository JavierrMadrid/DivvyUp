package com.example.divvyup.domain.model

import kotlin.time.Instant

data class Participant(
    val id: Long = 0,
    val groupId: Long,
    val name: String,
    val email: String? = null,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0)
)

