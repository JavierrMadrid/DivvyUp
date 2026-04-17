package com.example.divvyup.domain.model

import kotlin.time.Instant

data class InviteToken(
    val token: String,
    val groupId: Long,
    val createdByUserId: String,
    val expiresAt: Instant,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0)
)

