package com.example.divvyup.integration.cache

import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.repository.GroupRepository

/**
 * Decorador de caché para GroupRepository.
 * TTL = 2 minutos — los grupos cambian con poca frecuencia.
 *
 * Claves:
 *   - "all"      → lista completa (getAll)
 *   - id (Long)  → grupo individual (getById)
 */
class CachedGroupRepository(
    private val delegate: GroupRepository,
    ttlMillis: Long = 2 * 60_000L // 2 min
) : GroupRepository {

    private val listCache   = InMemoryCache<String, List<Group>>(ttlMillis)
    private val singleCache = InMemoryCache<Long, Group>(ttlMillis)

    override suspend fun getAll(): List<Group> =
        listCache.getOrLoad("all") { delegate.getAll() }

    override suspend fun getById(id: Long): Group =
        singleCache.getOrLoad(id) { delegate.getById(id) }

    override suspend fun create(group: Group): Group {
        val result = delegate.create(group)
        listCache.invalidate("all")
        return result
    }

    override suspend fun update(group: Group): Group {
        val result = delegate.update(group)
        listCache.invalidate("all")
        singleCache.invalidate(group.id)
        return result
    }

    override suspend fun delete(id: Long) {
        delegate.delete(id)
        listCache.invalidate("all")
        singleCache.invalidate(id)
    }
}

