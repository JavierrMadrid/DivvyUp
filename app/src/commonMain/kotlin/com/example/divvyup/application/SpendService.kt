@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.example.divvyup.application

import com.example.divvyup.domain.model.Recurrence
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.domain.repository.SpendRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.math.roundToLong

/**
 * Orquesta la lógica de creación/edición de gastos y cálculo de repartos.
 */
class SpendService(
    private val spendRepository: SpendRepository
) {

    suspend fun getSpends(groupId: Long): List<Spend> =
        spendRepository.getByGroup(groupId)

    suspend fun getSharesBySpend(spendId: Long): List<SpendShare> =
        spendRepository.getSharesBySpend(spendId)

    /**
     * Devuelve un mapa spendId → impacto neto para el participante dado.
     *
     * Impacto neto = (importe total si es pagador) - (share que le corresponde)
     *   · Valor positivo (+): pagó por otros → le deben
     *   · Valor negativo (-): debe su parte a quien pagó
     *
     * Si el participante no tiene share en un gasto, su impacto es 0 a menos que sea el pagador.
     */
    suspend fun getPersonalImpactByGroup(groupId: Long, participantId: Long): Map<Long, Double> {
        val spends = spendRepository.getByGroup(groupId)
        val myShares = spendRepository.getSharesByParticipant(participantId)
            .associateBy { it.spendId }          // spendId → SpendShare

        return spends.associate { spend ->
            val myShare = myShares[spend.id]?.amount ?: 0.0
            val paid = if (spend.payerId == participantId) spend.amount else 0.0
            spend.id to roundToTwoDecimals(paid - myShare)
        }
    }

    /**
     * Crea un gasto con reparto EQUAL entre los participantes seleccionados.
     * Si [participantIds] está vacío, reparte entre todos los del grupo.
     */
    suspend fun createEqualSpend(
        groupId: Long,
        concept: String,
        amount: Double,
        payerId: Long,
        participantIds: List<Long>,
        categoryId: Long? = null,
        notes: String = "",
        date: Instant = Clock.System.now(),
        recurrence: Recurrence = Recurrence.NONE,
        receiptUrl: String? = null
    ): Spend {
        require(concept.isNotBlank()) { "El concepto no puede estar vacío" }
        require(amount > 0) { "El importe debe ser mayor que cero" }
        require(participantIds.isNotEmpty()) { "Debe haber al menos un participante" }

        val shares = buildEqualShares(amount, participantIds)
        val spend = Spend(
            groupId = groupId,
            concept = concept.trim(),
            amount = amount,
            date = date,
            payerId = payerId,
            categoryId = categoryId,
            splitType = SplitType.EQUAL,
            notes = notes.trim(),
            recurrence = recurrence,
            receiptUrl = receiptUrl
        )
        return spendRepository.create(spend, shares)
    }

    /**
     * Crea un gasto con reparto PERCENTAGE.
     * [percentages] es un mapa participantId → porcentaje (deben sumar 100).
     */
    suspend fun createPercentageSpend(
        groupId: Long,
        concept: String,
        amount: Double,
        payerId: Long,
        percentages: Map<Long, Double>,
        categoryId: Long? = null,
        notes: String = "",
        date: Instant = Clock.System.now(),
        recurrence: Recurrence = Recurrence.NONE,
        receiptUrl: String? = null
    ): Spend {
        require(concept.isNotBlank()) { "El concepto no puede estar vacío" }
        require(amount > 0) { "El importe debe ser mayor que cero" }
        require(percentages.isNotEmpty()) { "Debe haber al menos un participante" }
        val totalPct = percentages.values.sum()
        require(totalPct in 99.99..100.01) { "Los porcentajes deben sumar 100 (suma actual: $totalPct)" }

        val shares = percentages.map { (participantId, pct) ->
            SpendShare(
                spendId = 0,
                participantId = participantId,
                amount = roundToTwoDecimals(amount * pct / 100.0),
                percentage = pct
            )
        }
        val spend = Spend(
            groupId = groupId, concept = concept.trim(), amount = amount,
            date = date, payerId = payerId, categoryId = categoryId,
            splitType = SplitType.PERCENTAGE, notes = notes.trim(),
            recurrence = recurrence, receiptUrl = receiptUrl
        )
        return spendRepository.create(spend, shares)
    }

    /**
     * Crea un gasto con reparto CUSTOM (importes fijos por participante).
     * [customAmounts] es un mapa participantId → importe.
     * La suma debe ser igual al importe total del gasto.
     */
    suspend fun createCustomSpend(
        groupId: Long,
        concept: String,
        amount: Double,
        payerId: Long,
        customAmounts: Map<Long, Double>,
        categoryId: Long? = null,
        notes: String = "",
        date: Instant = Clock.System.now(),
        recurrence: Recurrence = Recurrence.NONE,
        receiptUrl: String? = null
    ): Spend {
        require(concept.isNotBlank()) { "El concepto no puede estar vacío" }
        require(amount > 0) { "El importe debe ser mayor que cero" }
        require(customAmounts.isNotEmpty()) { "Debe haber al menos un participante" }
        val totalShares = customAmounts.values.sum()
        require(kotlin.math.abs(totalShares - amount) < 0.01) {
            "La suma de importes (${totalShares}) debe ser igual al total ($amount)"
        }

        val shares = customAmounts.map { (participantId, shareAmount) ->
            SpendShare(spendId = 0, participantId = participantId, amount = shareAmount)
        }
        val spend = Spend(
            groupId = groupId, concept = concept.trim(), amount = amount,
            date = date, payerId = payerId, categoryId = categoryId,
            splitType = SplitType.CUSTOM, notes = notes.trim(),
            recurrence = recurrence, receiptUrl = receiptUrl
        )
        return spendRepository.create(spend, shares)
    }

    suspend fun updateEqualSpend(
        existing: Spend,
        concept: String,
        amount: Double,
        payerId: Long,
        participantIds: List<Long>,
        categoryId: Long? = null,
        notes: String = existing.notes,
        date: Instant = existing.date,
        recurrence: Recurrence = existing.recurrence,
        receiptUrl: String? = existing.receiptUrl
    ): Spend {
        require(concept.isNotBlank()) { "El concepto no puede estar vacío" }
        require(amount > 0) { "El importe debe ser mayor que cero" }
        require(participantIds.isNotEmpty()) { "Debe haber al menos un participante" }
        val shares = buildEqualShares(amount, participantIds)
        val updated = existing.copy(
            concept = concept.trim(), amount = amount, payerId = payerId,
            categoryId = categoryId, splitType = SplitType.EQUAL,
            notes = preserveSettlementLinkOnUpdate(existingNotes = existing.notes, newNotes = notes),
            date = date, recurrence = recurrence, receiptUrl = receiptUrl
        )
        return spendRepository.update(updated, shares)
    }

    suspend fun updatePercentageSpend(
        existing: Spend,
        concept: String,
        amount: Double,
        payerId: Long,
        percentages: Map<Long, Double>,
        categoryId: Long? = null,
        notes: String = existing.notes,
        date: Instant = existing.date,
        recurrence: Recurrence = existing.recurrence,
        receiptUrl: String? = existing.receiptUrl
    ): Spend {
        require(concept.isNotBlank()) { "El concepto no puede estar vacío" }
        require(amount > 0) { "El importe debe ser mayor que cero" }
        require(percentages.isNotEmpty()) { "Debe haber al menos un participante" }
        val totalPct = percentages.values.sum()
        require(totalPct in 99.99..100.01) { "Los porcentajes deben sumar 100 (suma actual: $totalPct)" }
        val shares = percentages.map { (participantId, pct) ->
            SpendShare(spendId = existing.id, participantId = participantId,
                amount = roundToTwoDecimals(amount * pct / 100.0), percentage = pct)
        }
        val updated = existing.copy(
            concept = concept.trim(), amount = amount, payerId = payerId,
            categoryId = categoryId, splitType = SplitType.PERCENTAGE,
            notes = preserveSettlementLinkOnUpdate(existingNotes = existing.notes, newNotes = notes),
            date = date, recurrence = recurrence, receiptUrl = receiptUrl
        )
        return spendRepository.update(updated, shares)
    }

    suspend fun updateCustomSpend(
        existing: Spend,
        concept: String,
        amount: Double,
        payerId: Long,
        customAmounts: Map<Long, Double>,
        categoryId: Long? = null,
        notes: String = existing.notes,
        date: Instant = existing.date,
        recurrence: Recurrence = existing.recurrence,
        receiptUrl: String? = existing.receiptUrl
    ): Spend {
        require(concept.isNotBlank()) { "El concepto no puede estar vacío" }
        require(amount > 0) { "El importe debe ser mayor que cero" }
        require(customAmounts.isNotEmpty()) { "Debe haber al menos un participante" }
        val totalShares = customAmounts.values.sum()
        require(kotlin.math.abs(totalShares - amount) < 0.01) {
            "La suma de importes ($totalShares) debe ser igual al total ($amount)"
        }
        val shares = customAmounts.map { (participantId, shareAmount) ->
            SpendShare(spendId = existing.id, participantId = participantId, amount = shareAmount)
        }
        val updated = existing.copy(
            concept = concept.trim(), amount = amount, payerId = payerId,
            categoryId = categoryId, splitType = SplitType.CUSTOM,
            notes = preserveSettlementLinkOnUpdate(existingNotes = existing.notes, newNotes = notes),
            date = date, recurrence = recurrence, receiptUrl = receiptUrl
        )
        return spendRepository.update(updated, shares)
    }

    suspend fun deleteSpend(id: Long) =
        spendRepository.delete(id)

    /**
     * Borra gastos del grupo que cumplan los criterios dados (AND entre criterios no nulos).
     * [beforeInstant]: si se indica, solo se borran gastos cuya fecha sea ANTERIOR a ese instante.
     * Si todos los filtros son nulos, borra todos los gastos del grupo.
     */
    suspend fun deleteSpendsFiltered(
        groupId: Long,
        categoryId: Long? = null,
        payerId: Long? = null,
        beforeInstant: Instant? = null
    ) {
        val all = spendRepository.getByGroup(groupId)
        val toDelete = all.filter { spend ->
            val catOk    = categoryId    == null || spend.categoryId == categoryId
            val payerOk  = payerId       == null || spend.payerId    == payerId
            val dateOk   = beforeInstant == null || spend.date       < beforeInstant
            catOk && payerOk && dateOk
        }
        if (toDelete.isNotEmpty()) {
            spendRepository.deleteAll(toDelete.map { it.id })
        }
    }

    /** Borra una lista de gastos por sus IDs concretos. */
    suspend fun deleteSpendsByIds(ids: List<Long>) {
        if (ids.isNotEmpty()) spendRepository.deleteAll(ids)
    }

    // --- Helpers ---

    private fun buildEqualShares(amount: Double, participantIds: List<Long>): List<SpendShare> {
        val n = participantIds.size
        val baseShare = roundToTwoDecimals(amount / n)
        // Ajuste del último participante para absorber el centavo de redondeo
        val remainder = roundToTwoDecimals(amount - baseShare * (n - 1))
        return participantIds.mapIndexed { index, participantId ->
            SpendShare(
                spendId = 0,
                participantId = participantId,
                amount = if (index == n - 1) remainder else baseShare
            )
        }
    }

    private fun roundToTwoDecimals(value: Double): Double =
        (value * 100).roundToLong() / 100.0

    private fun preserveSettlementLinkOnUpdate(existingNotes: String, newNotes: String): String {
        val normalizedNew = newNotes.trim()
        if (!existingNotes.startsWith(SETTLEMENT_NOTE_PREFIX)) {
            return if (normalizedNew.isNotEmpty()) normalizedNew else existingNotes
        }

        val settlementToken = existingNotes.substringBefore("|")
        if (normalizedNew.isEmpty()) return existingNotes
        if (normalizedNew.startsWith(SETTLEMENT_NOTE_PREFIX)) return normalizedNew

        return "$settlementToken|$normalizedNew"
    }
}
