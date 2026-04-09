package com.example.divvyup.domain.model

import kotlin.time.Instant

data class Settlement(
    val id: Long = 0,
    val groupId: Long,
    val fromParticipantId: Long,
    val toParticipantId: Long,
    val amount: Double,
    val date: Instant = Instant.fromEpochMilliseconds(0),
    val notes: String = "",
    val createdAt: Instant = Instant.fromEpochMilliseconds(0)
)

