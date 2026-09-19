package com.example.divvyup.integration.cache

import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.repository.SpendCursor
import com.example.divvyup.domain.repository.SpendPage
import com.example.divvyup.domain.repository.SpendRepository
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Decorador de caché para SpendRepository.
 * TTL = 1 minuto — los gastos cambian con más frecuencia.
 *
 * Claves:
 *   - groupId (Long)    → lista de gastos del grupo (getByGroup), TTL en memoria
 *   - spendId (Long)    → shares de un gasto (getSharesBySpend), TTL separado
 *   - participantId (Long) → todas las shares de un participante (getSharesByParticipant)
 *
 * La primera página de [getSpendsPage] se persiste además en [startupCache]
 * (caché de arranque), para que la lista pinte de forma instantánea al abrir la app.
 */
class CachedSpendRepository(
    private val delegate: SpendRepository,
    private val startupCache: SpendStartupCache? = null,
    ttlMillis: Long = 60_000L // 1 min
) : SpendRepository {

    private val spendCache = InMemoryCache<Long, List<Spend>>(ttlMillis)
    private val lastSpendDateCache = InMemoryCache<Long, Instant?>(ttlMillis)
    private val sharesCache = InMemoryCache<Long, List<SpendShare>>(ttlMillis)
    private val groupSharesCache = InMemoryCache<Long, List<SpendShare>>(ttlMillis)
    private val participantSharesCache = InMemoryCache<Long, List<SpendShare>>(ttlMillis)

    override suspend fun getByGroup(groupId: Long): List<Spend> =
        spendCache.getOrLoad(groupId) { delegate.getByGroup(groupId) }

    override suspend fun getSpendsPage(groupId: Long, pageSize: Int, before: SpendCursor?): SpendPage {
        val page = delegate.getSpendsPage(groupId, pageSize, before)
        // Write-through: la primera página fresca se persiste para el arranque instantáneo.
        if (before == null && startupCache != null) {
            try {
                startupCache.save(
                    groupId,
                    SpendStartupEntry(items = page.items, hasMore = page.hasMore, savedAt = Clock.System.now())
                )
            } catch (_: Exception) {
                println("DEBUG CachedSpendRepository: no se pudo persistir la caché de arranque")
            }
        }
        return page
    }

    override suspend fun getLastSpendDate(groupId: Long): Instant? =
        lastSpendDateCache.getOrLoad(groupId) { delegate.getLastSpendDate(groupId) }

    override suspend fun getSharesBySpend(spendId: Long): List<SpendShare> =
        sharesCache.getOrLoad(spendId) { delegate.getSharesBySpend(spendId) }

    override suspend fun getSharesBySpendIds(spendIds: List<Long>): List<SpendShare> =
        delegate.getSharesBySpendIds(spendIds)

    override suspend fun getSharesByGroup(groupId: Long): List<SpendShare> =
        groupSharesCache.getOrLoad(groupId) { delegate.getSharesByGroup(groupId) }

    override suspend fun getSharesByParticipant(participantId: Long): List<SpendShare> =
        participantSharesCache.getOrLoad(participantId) { delegate.getSharesByParticipant(participantId) }

    override suspend fun create(spend: Spend, shares: List<SpendShare>): Spend {
        val result = delegate.create(spend, shares)
        spendCache.invalidate(spend.groupId)
        lastSpendDateCache.invalidate(spend.groupId)
        groupSharesCache.invalidate(spend.groupId)
        participantSharesCache.clear()
        startupCache?.clear(spend.groupId)
        return result
    }

    override suspend fun update(spend: Spend, shares: List<SpendShare>): Spend {
        val result = delegate.update(spend, shares)
        spendCache.invalidate(spend.groupId)
        lastSpendDateCache.invalidate(spend.groupId)
        sharesCache.invalidate(spend.id)
        groupSharesCache.invalidate(spend.groupId)
        participantSharesCache.clear()
        startupCache?.clear(spend.groupId)
        return result
    }

    override suspend fun delete(id: Long) {
        spendCache.clear()
        lastSpendDateCache.clear()
        sharesCache.invalidate(id)
        groupSharesCache.clear()
        participantSharesCache.clear()
        startupCache?.clearAll()
        delegate.delete(id)
    }

    override suspend fun deleteAll(ids: List<Long>) {
        spendCache.clear()
        lastSpendDateCache.clear()
        ids.forEach { sharesCache.invalidate(it) }
        groupSharesCache.clear()
        participantSharesCache.clear()
        startupCache?.clearAll()
        delegate.deleteAll(ids)
    }

    /** Los gastos recurrentes vencidos se consultan siempre al delegate (sin caché) para evitar
     *  que un TTL de 1 min bloquee la materialización. */
    override suspend fun getRecurringRootsDue(groupId: Long, dueBeforeOrAt: Instant): List<Spend> =
        delegate.getRecurringRootsDue(groupId, dueBeforeOrAt)

    override suspend fun updateNextDue(spendId: Long, nextDue: Instant) {
        spendCache.clear()           // fuerza recarga para que el cambio se refleje
        lastSpendDateCache.clear()
        delegate.updateNextDue(spendId, nextDue)
    }

    /** Limpia toda la caché (útil al cambiar de sesión). */
    fun clearAll() {
        spendCache.clear()
        lastSpendDateCache.clear()
        sharesCache.clear()
        groupSharesCache.clear()
        participantSharesCache.clear()
        startupCache?.clearAll()
    }
}

