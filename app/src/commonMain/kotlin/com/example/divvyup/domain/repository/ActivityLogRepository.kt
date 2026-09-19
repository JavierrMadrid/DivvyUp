package com.example.divvyup.domain.repository
import com.example.divvyup.domain.model.ActivityLog
import kotlin.time.Instant
interface ActivityLogRepository {
    suspend fun getByGroup(groupId: Long): List<ActivityLog>
    suspend fun log(entry: ActivityLog): ActivityLog
    /** Elimina entradas del grupo anteriores a [cutoff]. */
    suspend fun deleteOlderThan(groupId: Long, cutoff: Instant)
}
