package com.example.divvyup.integration.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParticipantUserLinkDto(
    val id: Long = 0,
    @SerialName("group_id") val groupId: Long,
    @SerialName("participant_id") val participantId: Long,
    @SerialName("user_id") val userId: String
)

