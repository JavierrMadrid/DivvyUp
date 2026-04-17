package com.example.divvyup.integration.supabase.dto

import com.example.divvyup.domain.model.Group
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// DTOs con @Serializable para Supabase/kotlinx.serialization
// Los mappers están como extension functions junto al DTO (skill: android-clean-architecture — Mapper Pattern)

@Serializable
data class GroupDto(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val currency: String = "EUR",
    @SerialName("owner_user_id") val ownerUserId: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

fun GroupDto.toDomain() = Group(
    id = id,
    name = name,
    description = description,
    currency = currency,
    ownerUserId = ownerUserId,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt)
                else Instant.fromEpochMilliseconds(0)
)

fun Group.toDto() = GroupDto(
    id = id,
    name = name,
    description = description,
    currency = currency,
    ownerUserId = ownerUserId
)

/** DTO para UPDATE: excluye `id` y `created_at` para evitar el error
 *  "column id can only be updated to default" en columnas GENERATED ALWAYS. */
@Serializable
data class GroupUpdateDto(
    val name: String,
    val description: String,
    val currency: String
)

fun Group.toUpdateDto() = GroupUpdateDto(
    name = name,
    description = description,
    currency = currency
)
