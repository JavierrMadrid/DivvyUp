package com.example.divvyup.domain.model

import kotlin.time.Instant

data class SpendShare(
    val id: Long = 0,
    val spendId: Long,
    val participantId: Long,
    val amount: Double,
    val percentage: Double? = null,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0)
)

