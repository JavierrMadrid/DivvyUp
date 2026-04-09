package com.example.divvyup.domain.repository

import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Settlement

interface SettlementRepository {
    suspend fun getByGroup(groupId: Long): List<Settlement>
    suspend fun create(settlement: Settlement): Settlement
    suspend fun delete(id: Long)
    /** Devuelve los balances netos calculados por la vista participant_balances */
    suspend fun getBalances(groupId: Long): List<ParticipantBalance>
}

