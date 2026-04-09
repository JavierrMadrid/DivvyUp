package com.example.divvyup.integration.cache

import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.repository.SpendRepository

/**
 * Decorador de caché para SpendRepository.
 * TTL = 1 minuto — los gastos cambian con más frecuencia.
 *
 * Claves:
 *   - groupId (Long)  → lista de gastos del grupo (getByGroup)
 *   - spendId (Long)  → shares de un gasto (getSharesBySpend), TTL separado
 */
class CachedSpendRepository(
    private val delegate: SpendRepository,
    ttlMillis: Long = 60_000L // 1 min
) : SpendRepository {

    private val spendCache = InMemoryCache<Long, List<Spend>>(ttlMillis)
    private val sharesCache = InMemoryCache<Long, List<SpendShare>>(ttlMillis)

    override suspend fun getByGroup(groupId: Long): List<Spend> =
        spendCache.getOrLoad(groupId) { delegate.getByGroup(groupId) }

    override suspend fun getSharesBySpend(spendId: Long): List<SpendShare> =
        sharesCache.getOrLoad(spendId) { delegate.getSharesBySpend(spendId) }

    override suspend fun create(spend: Spend, shares: List<SpendShare>): Spend {
        val result = delegate.create(spend, shares)
        spendCache.invalidate(spend.groupId)
        return result
    }

    override suspend fun update(spend: Spend, shares: List<SpendShare>): Spend {
        val result = delegate.update(spend, shares)
        spendCache.invalidate(spend.groupId)
        sharesCache.invalidate(spend.id)
        return result
    }

    override suspend fun delete(id: Long) {
        spendCache.clear()
        sharesCache.invalidate(id)
        delegate.delete(id)
    }

    override suspend fun deleteAll(ids: List<Long>) {
        spendCache.clear()
        ids.forEach { sharesCache.invalidate(it) }
        delegate.deleteAll(ids)
    }
}

