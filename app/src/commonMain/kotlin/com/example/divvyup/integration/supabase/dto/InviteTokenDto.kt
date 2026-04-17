package com.example.divvyup.integration.supabase.dto

import com.example.divvyup.domain.model.InviteToken
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InviteTokenDto(
    val token: String = "",
    @SerialName("group_id") val groupId: Long,
    @SerialName("created_by_user_id") val createdByUserId: String,
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

fun InviteTokenDto.toDomain() = InviteToken(
    token = token,
    groupId = groupId,
    createdByUserId = createdByUserId,
    expiresAt = if (expiresAt.isNotEmpty()) Instant.parse(expiresAt) else Instant.fromEpochMilliseconds(0),
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt) else Instant.fromEpochMilliseconds(0)
)

