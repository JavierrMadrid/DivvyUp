package com.example.divvyup.integration.supabase.dto

import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SpendDto(
    val id: Long = 0,
    @SerialName("group_id") val groupId: Long,
    val concept: String,
    val amount: Double,
    val date: String = "",
    @SerialName("payer_id") val payerId: Long,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("split_type") val splitType: String = "EQUAL",
    val notes: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

fun SpendDto.toDomain() = Spend(
    id = id,
    groupId = groupId,
    concept = concept,
    amount = amount,
    date = if (date.isNotEmpty()) Instant.parse(date) else Instant.fromEpochMilliseconds(0),
    payerId = payerId,
    categoryId = categoryId,
    splitType = SplitType.valueOf(splitType),
    notes = notes,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt)
                else Instant.fromEpochMilliseconds(0)
)

fun Spend.toDto() = SpendDto(
    id = id,
    groupId = groupId,
    concept = concept,
    amount = amount,
    date = date.toString(),
    payerId = payerId,
    categoryId = categoryId,
    splitType = splitType.name,
    notes = notes
)

/** DTO sin id para UPDATE — Supabase rechaza actualizar columnas identity. */
@Serializable
data class SpendUpdateDto(
    @SerialName("group_id")    val groupId: Long,
    val concept: String,
    val amount: Double,
    val date: String,
    @SerialName("payer_id")    val payerId: Long,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("split_type")  val splitType: String,
    val notes: String
)

fun Spend.toUpdateDto() = SpendUpdateDto(
    groupId    = groupId,
    concept    = concept,
    amount     = amount,
    date       = date.toString(),
    payerId    = payerId,
    categoryId = categoryId,
    splitType  = splitType.name,
    notes      = notes
)

// --- SpendShare DTO ---

@Serializable
data class SpendShareDto(
    val id: Long = 0,
    @SerialName("spend_id") val spendId: Long,
    @SerialName("participant_id") val participantId: Long,
    val amount: Double,
    val percentage: Double? = null,
    @SerialName("created_at") val createdAt: String = ""
)

fun SpendShareDto.toDomain() = SpendShare(
    id = id,
    spendId = spendId,
    participantId = participantId,
    amount = amount,
    percentage = percentage,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt)
                else Instant.fromEpochMilliseconds(0)
)

fun SpendShare.toDto(resolvedSpendId: Long = spendId) = SpendShareDto(
    id = id,
    spendId = resolvedSpendId,
    participantId = participantId,
    amount = amount,
    percentage = percentage
)

