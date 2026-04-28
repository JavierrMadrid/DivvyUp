package com.example.divvyup.integration.cache
import com.example.divvyup.domain.model.ActivityLog
import com.example.divvyup.domain.repository.ActivityLogRepository
import kotlin.time.Instant
class CachedActivityLogRepository(
    private val delegate: ActivityLogRepository,
    ttlMillis: Long = 30_000L
) : ActivityLogRepository {
    private val cache = InMemoryCache<Long, List<ActivityLog>>(ttlMillis)
    override suspend fun getByGroup(groupId: Long): List<ActivityLog> =
        cache.getOrLoad(groupId) { delegate.getByGroup(groupId) }
    override suspend fun log(entry: ActivityLog): ActivityLog {
        cache.invalidate(entry.groupId)
        return delegate.log(entry)
    }
    override suspend fun deleteOlderThan(groupId: Long, cutoff: Instant) {
        cache.invalidate(groupId)
        delegate.deleteOlderThan(groupId, cutoff)
    }
}

