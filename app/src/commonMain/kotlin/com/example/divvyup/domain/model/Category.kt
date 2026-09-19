package com.example.divvyup.domain.model

import kotlin.time.Instant

data class Category(
    val id: Long = 0,
    val groupId: Long? = null,       // null = categoría global predefinida
    val name: String,
    val icon: String = "📦",
    val color: String = "#6366F1",
    val isDefault: Boolean = false,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0),
    /** Presupuesto mensual opcional (null = sin límite). */
    val budget: Double? = null
)

