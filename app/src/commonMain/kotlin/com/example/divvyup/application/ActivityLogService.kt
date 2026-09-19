@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.example.divvyup.application

import com.example.divvyup.domain.model.ActivityEventType
import com.example.divvyup.domain.model.ActivityLog
import com.example.divvyup.domain.repository.ActivityLogRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

private const val RETENTION_DAYS = 30

class ActivityLogService(
    private val repository: ActivityLogRepository
) {
    suspend fun getActivityLog(groupId: Long): List<ActivityLog> {
        val cutoff = Clock.System.now() - RETENTION_DAYS.days
        repository.deleteOlderThan(groupId, cutoff)
        return repository.getByGroup(groupId)
    }

    suspend fun logEvent(
        groupId: Long,
        eventType: ActivityEventType,
        description: String,
        actorParticipantId: Long? = null,
        actorName: String? = null,
        entityId: Long? = null
    ) {
        repository.log(
            ActivityLog(
                groupId = groupId,
                actorParticipantId = actorParticipantId,
                actorName = actorName,
                eventType = eventType,
                entityId = entityId,
                description = description
            )
        )
    }
}

