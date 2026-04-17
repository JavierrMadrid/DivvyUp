package com.example.divvyup.integration.supabase.dto

import com.example.divvyup.domain.model.Participant
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParticipantDto(
    val id: Long = 0,
    @SerialName("group_id") val groupId: Long,
    val name: String,
    val email: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

fun ParticipantDto.toDomain() = Participant(
    id = id,
    groupId = groupId,
    name = name,
    email = email,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt)
                else Instant.fromEpochMilliseconds(0)
)

fun Participant.toDto() = ParticipantDto(
    id = id,
    groupId = groupId,
    name = name,
    email = email
)

