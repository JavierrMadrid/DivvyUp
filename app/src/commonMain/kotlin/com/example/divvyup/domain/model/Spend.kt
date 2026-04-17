package com.example.divvyup.domain.model

import kotlin.time.Instant

data class Spend(
    val id: Long = 0,
    val groupId: Long,
    val concept: String,
    val amount: Double,
    val date: Instant = Instant.fromEpochMilliseconds(0),
    val payerId: Long,
    val categoryId: Long? = null,
    val splitType: SplitType = SplitType.EQUAL,
    val notes: String = "",
    val createdAt: Instant = Instant.fromEpochMilliseconds(0)
)

