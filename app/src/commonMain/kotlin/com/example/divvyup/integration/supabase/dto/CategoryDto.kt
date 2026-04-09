package com.example.divvyup.integration.supabase.dto

import com.example.divvyup.domain.model.Category
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: Long = 0,
    @SerialName("group_id") val groupId: Long? = null,
    val name: String,
    val icon: String = "📦",
    val color: String = "#6366F1",
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)

fun CategoryDto.toDomain() = Category(
    id = id,
    groupId = groupId,
    name = name,
    icon = icon,
    color = color,
    isDefault = isDefault,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt)
                else Instant.fromEpochMilliseconds(0)
)

fun Category.toDto() = CategoryDto(
    id = id,
    groupId = groupId,
    name = name,
    icon = icon,
    color = color,
    isDefault = isDefault
)

