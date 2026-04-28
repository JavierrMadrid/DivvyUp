package com.example.divvyup.domain.model

import kotlin.time.Instant

data class SpendComment(
    val id: Long = 0,
    val spendId: Long,
    val participantId: Long,
    val text: String,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0)
)

