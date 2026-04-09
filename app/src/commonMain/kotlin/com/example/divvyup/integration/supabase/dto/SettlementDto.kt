package com.example.divvyup.integration.supabase.dto

import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Settlement
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SettlementDto(
    val id: Long = 0,
    @SerialName("group_id") val groupId: Long,
    @SerialName("from_participant_id") val fromParticipantId: Long,
    @SerialName("to_participant_id") val toParticipantId: Long,
    val amount: Double,
    val date: String = "",
    val notes: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

fun SettlementDto.toDomain() = Settlement(
    id = id,
    groupId = groupId,
    fromParticipantId = fromParticipantId,
    toParticipantId = toParticipantId,
    amount = amount,
    date = if (date.isNotEmpty()) Instant.parse(date) else Instant.fromEpochMilliseconds(0),
    notes = notes,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt)
                else Instant.fromEpochMilliseconds(0)
)

fun Settlement.toDto() = SettlementDto(
    id = id,
    groupId = groupId,
    fromParticipantId = fromParticipantId,
    toParticipantId = toParticipantId,
    amount = amount,
    date = date.toString(),
    notes = notes
)

// --- Balance DTO (de la vista participant_balances) ---

@Serializable
data class ParticipantBalanceDto(
    @SerialName("participant_id") val participantId: Long,
    @SerialName("group_id") val groupId: Long,
    @SerialName("participant_name") val participantName: String,
    @SerialName("total_paid") val totalPaid: Double,
    @SerialName("total_owed") val totalOwed: Double,
    @SerialName("settlements_received") val settlementsReceived: Double,
    @SerialName("settlements_sent") val settlementsSent: Double,
    @SerialName("net_balance") val netBalance: Double
)

fun ParticipantBalanceDto.toDomain() = ParticipantBalance(
    participantId = participantId,
    groupId = groupId,
    participantName = participantName,
    totalPaid = totalPaid,
    totalOwed = totalOwed,
    settlementsReceived = settlementsReceived,
    settlementsSent = settlementsSent,
    netBalance = netBalance
)

