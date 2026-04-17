package com.example.divvyup.integration.cache

import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.repository.SettlementRepository

/**
 * Decorador de caché para SettlementRepository.
 * TTL = 1 minuto — los balances se calculan en BD (vista), son costosos de computar.
 *
 * Claves:
 *   - "settlements_<groupId>"  → lista de liquidaciones
 *   - "balances_<groupId>"     → balances netos calculados por la vista
 */
class CachedSettlementRepository(
    private val delegate: SettlementRepository,
    ttlMillis: Long = 60_000L // 1 min
) : SettlementRepository {

    private val settlementsCache = InMemoryCache<Long, List<Settlement>>(ttlMillis)
    private val balancesCache    = InMemoryCache<Long, List<ParticipantBalance>>(ttlMillis)

    override suspend fun getByGroup(groupId: Long): List<Settlement> =
        settlementsCache.getOrLoad(groupId) { delegate.getByGroup(groupId) }

    override suspend fun getBalances(groupId: Long): List<ParticipantBalance> =
        balancesCache.getOrLoad(groupId) { delegate.getBalances(groupId) }

    override suspend fun create(settlement: Settlement): Settlement {
        val result = delegate.create(settlement)
        // Una nueva liquidación afecta tanto liquidaciones como balances del grupo
        settlementsCache.invalidate(settlement.groupId)
        balancesCache.invalidate(settlement.groupId)
        return result
    }

    override suspend fun delete(id: Long) {
        // Sin groupId — limpiamos toda la caché
        settlementsCache.clear()
        balancesCache.clear()
        delegate.delete(id)
    }
}

