package com.example.divvyup.domain.model

import kotlin.time.Instant

enum class ActivityEventType {
    GASTO_CREADO,
    GASTO_EDITADO,
    GASTO_ELIMINADO,
    PARTICIPANTE_ANADIDO,
    LIQUIDACION_CREADA,
    LIQUIDACION_ELIMINADA
}

data class ActivityLog(
    val id: Long = 0,
    val groupId: Long,
    val actorParticipantId: Long? = null,
    val actorName: String? = null,
    val eventType: ActivityEventType,
    val entityId: Long? = null,
    val description: String,
    val createdAt: Instant = Instant.fromEpochMilliseconds(0)
)

