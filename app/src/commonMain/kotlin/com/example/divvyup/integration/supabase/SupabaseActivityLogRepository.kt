@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.example.divvyup.integration.supabase

import com.example.divvyup.domain.model.ActivityEventType
import com.example.divvyup.domain.model.ActivityLog
import com.example.divvyup.domain.repository.ActivityLogRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private const val RETENTION_DAYS = 30

@Serializable
private data class ActivityLogDto(
    val id: Long = 0,
    @SerialName("group_id")             val groupId: Long,
    @SerialName("actor_participant_id") val actorParticipantId: Long? = null,
    @SerialName("actor_name")           val actorName: String? = null,
    @SerialName("event_type")           val eventType: String,
    @SerialName("entity_id")            val entityId: Long? = null,
    val description: String,
    @SerialName("created_at")           val createdAt: String = ""
)

@Serializable
private data class ActivityLogInsertDto(
    @SerialName("group_id")             val groupId: Long,
    @SerialName("actor_participant_id") val actorParticipantId: Long? = null,
    @SerialName("actor_name")           val actorName: String? = null,
    @SerialName("event_type")           val eventType: String,
    @SerialName("entity_id")            val entityId: Long? = null,
    val description: String
)

private fun ActivityLogDto.toDomain() = ActivityLog(
    id = id,
    groupId = groupId,
    actorParticipantId = actorParticipantId,
    actorName = actorName,
    eventType = runCatching { ActivityEventType.valueOf(eventType) }.getOrDefault(ActivityEventType.GASTO_CREADO),
    entityId = entityId,
    description = description,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt) else Instant.fromEpochMilliseconds(0)
)

class SupabaseActivityLogRepository(
    private val postgrest: Postgrest
) : ActivityLogRepository {

    override suspend fun getByGroup(groupId: Long): List<ActivityLog> = try {
        val cutoff = Clock.System.now() - RETENTION_DAYS.days
        postgrest.from("activity_log")
            .select {
                filter {
                    eq("group_id", groupId)
                    gte("created_at", cutoff.toString())
                }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(100)
            }
            .decodeList<ActivityLogDto>()
            .map { it.toDomain() }
    } catch (e: Exception) {
        throw Exception("Error al obtener historial: ${e.message}", e)
    }

    override suspend fun deleteOlderThan(groupId: Long, cutoff: Instant) {
        try {
            postgrest.from("activity_log")
                .delete {
                    filter {
                        eq("group_id", groupId)
                        lt("created_at", cutoff.toString())
                    }
                }
        } catch (e: Exception) {
            println("DEBUG SupabaseActivityLogRepository: deleteOlderThan error — ${e.message}")
        }
    }

    override suspend fun log(entry: ActivityLog): ActivityLog = try {
        postgrest.from("activity_log")
            .insert(ActivityLogInsertDto(
                groupId = entry.groupId,
                actorParticipantId = entry.actorParticipantId,
                actorName = entry.actorName,
                eventType = entry.eventType.name,
                entityId = entry.entityId,
                description = entry.description
            )) { select() }
            .decodeSingle<ActivityLogDto>()
            .toDomain()
    } catch (e: Exception) {
        // El log de actividad es no-crítico: no relanzamos
        println("DEBUG SupabaseActivityLogRepository: log error — ${e.message}")
        entry
    }
}

