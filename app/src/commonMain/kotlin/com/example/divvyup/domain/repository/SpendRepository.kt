package com.example.divvyup.domain.repository

import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import kotlin.time.Instant

interface SpendRepository {
    suspend fun getByGroup(groupId: Long): List<Spend>
    suspend fun getSharesBySpend(spendId: Long): List<SpendShare>
    /** Devuelve todas las shares de todos los gastos de un grupo. */
    suspend fun getSharesByGroup(groupId: Long): List<SpendShare>
    /** Devuelve todas las shares en las que participa [participantId] (para calcular impacto personal). */
    suspend fun getSharesByParticipant(participantId: Long): List<SpendShare>
    /** Crea el gasto y sus shares en una sola operación */
    suspend fun create(spend: Spend, shares: List<SpendShare>): Spend
    suspend fun update(spend: Spend, shares: List<SpendShare>): Spend
    suspend fun delete(id: Long)
    /** Borra una lista de gastos por sus IDs */
    suspend fun deleteAll(ids: List<Long>)
    /**
     * Devuelve los gastos raíz recurrentes del grupo cuyos [recurrenceNextDue] ≤ [dueBeforeOrAt].
     * Un gasto raíz tiene recurrence ≠ NONE y recurrenceParentId == null.
     */
    suspend fun getRecurringRootsDue(groupId: Long, dueBeforeOrAt: Instant): List<Spend>
    /** Actualiza solo el campo [recurrenceNextDue] del gasto raíz indicado. */
    suspend fun updateNextDue(spendId: Long, nextDue: Instant)
}
