package com.example.divvyup.integration.cache

import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.repository.ParticipantRepository

/**
 * Decorador de caché para ParticipantRepository.
 * TTL = 2 minutos — se añaden poco, se leen en cada pantalla.
 *
 * Clave de caché: groupId (Long).
 */
class CachedParticipantRepository(
    private val delegate: ParticipantRepository,
    ttlMillis: Long = 2 * 60_000L // 2 min
) : ParticipantRepository {

    private val cache = InMemoryCache<Long, List<Participant>>(ttlMillis)

    override suspend fun getByGroup(groupId: Long): List<Participant> =
        cache.getOrLoad(groupId) { delegate.getByGroup(groupId) }

    override suspend fun create(participant: Participant): Participant {
        val result = delegate.create(participant)
        cache.invalidate(participant.groupId)
        return result
    }

    override suspend fun update(participant: Participant): Participant {
        val result = delegate.update(participant)
        cache.invalidate(participant.groupId)
        return result
    }

    override suspend fun delete(id: Long) {
        // No tenemos groupId aquí; limpiamos toda la caché
        cache.clear()
        delegate.delete(id)
    }

    /** Limpia toda la caché (útil al cambiar de sesión). */
    fun clearAll() = cache.clear()
}

