@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.example.divvyup.application

import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.DebtTransfer
import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.domain.repository.CategoryRepository
import com.example.divvyup.domain.repository.ParticipantRepository
import com.example.divvyup.domain.repository.SettlementRepository
import com.example.divvyup.domain.repository.SpendRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Servicio de balances y liquidaciones.
 * Incluye el algoritmo greedy de minimización de transferencias.
 * Puro Kotlin — sin imports de framework (KMP commonMain).
 */
class SettlementService(
    private val settlementRepository: SettlementRepository,
    private val spendRepository: SpendRepository,
    private val participantRepository: ParticipantRepository,
    private val categoryRepository: CategoryRepository
) {

    private companion object {
        const val SETTLEMENT_CATEGORY_NAME = "Liquidación"
        const val SETTLEMENT_CATEGORY_ICON = "💸"
        const val SETTLEMENT_CATEGORY_COLOR = "#14B8A6"
        const val SETTLEMENT_SPEND_CONCEPT = "Liquidación"
        const val SETTLEMENT_NOTE_PREFIX = "__settlement_id:"
    }

    suspend fun getSettlements(groupId: Long): List<Settlement> =
        settlementRepository.getByGroup(groupId)

    suspend fun getBalances(groupId: Long): List<ParticipantBalance> {
        val participants = participantRepository.getByGroup(groupId)
        val spends = spendRepository.getByGroup(groupId)
        val settlements = settlementRepository.getByGroup(groupId)

        val shares = spends.flatMap { spend ->
            spendRepository.getSharesBySpend(spend.id)
        }

        val mirroredSettlementIds = spends.mapNotNull { extractMirroredSettlementId(it.notes) }.toSet()
        val unmatchedSettlements = settlements.filter { it.id !in mirroredSettlementIds }

        return participants.map { participant ->
            val totalPaid = spends
                .filter { it.payerId == participant.id }
                .sumOf { it.amount }
                .roundMoney()

            val totalOwed = shares
                .filter { it.participantId == participant.id }
                .sumOf { it.amount }
                .roundMoney()

            val settlementsReceived = settlements
                .filter { it.toParticipantId == participant.id }
                .sumOf { it.amount }
                .roundMoney()

            val settlementsSent = settlements
                .filter { it.fromParticipantId == participant.id }
                .sumOf { it.amount }
                .roundMoney()

            val unmatchedReceived = unmatchedSettlements
                .filter { it.toParticipantId == participant.id }
                .sumOf { it.amount }

            val unmatchedSent = unmatchedSettlements
                .filter { it.fromParticipantId == participant.id }
                .sumOf { it.amount }

            ParticipantBalance(
                participantId = participant.id,
                groupId = participant.groupId,
                participantName = participant.name,
                totalPaid = totalPaid,
                totalOwed = totalOwed,
                settlementsReceived = settlementsReceived,
                settlementsSent = settlementsSent,
                netBalance = (totalPaid - totalOwed - unmatchedReceived + unmatchedSent).roundMoney()
            )
        }
    }

    suspend fun createSettlement(
        groupId: Long,
        fromParticipantId: Long,
        toParticipantId: Long,
        amount: Double,
        notes: String = "",
        date: Instant = Clock.System.now()
    ): Settlement {
        require(fromParticipantId != toParticipantId) { "No puedes liquidar contigo mismo" }
        require(amount > 0) { "El importe debe ser mayor que cero" }

        val createdSettlement = settlementRepository.create(
            Settlement(
                groupId = groupId,
                fromParticipantId = fromParticipantId,
                toParticipantId = toParticipantId,
                amount = amount,
                date = date,
                notes = notes.trim()
            )
        )

        try {
            val settlementCategory = ensureSettlementCategory(groupId)
            createSettlementSpend(createdSettlement, settlementCategory.id)
            return createdSettlement
        } catch (e: Exception) {
            try {
                settlementRepository.delete(createdSettlement.id)
            } catch (rollbackError: Exception) {
                throw Exception(
                    "Error al registrar la liquidación completa y no se pudo revertir el settlement: ${rollbackError.message}",
                    e
                )
            }
            throw Exception("Error al crear el gasto asociado a la liquidación: ${e.message}", e)
        }
    }

    suspend fun deleteSettlement(id: Long) =
        settlementRepository.delete(id)

    suspend fun deleteSettlement(groupId: Long, id: Long) {
        val mirroredSpendIds = spendRepository.getByGroup(groupId)
            .asSequence()
            .filter { extractMirroredSettlementId(it.notes) == id }
            .map { it.id }
            .toList()

        if (mirroredSpendIds.isNotEmpty()) {
            spendRepository.deleteAll(mirroredSpendIds)
        }
        settlementRepository.delete(id)
    }

    /**
     * Algoritmo greedy de minimización de transferencias.
     * Dado una lista de balances netos, calcula el conjunto mínimo de
     * transferencias para que todos queden a 0.
     *
     * Complejidad: O(n log n) por el ordenado + O(n) por el bucle greedy.
     */
    fun simplifyDebts(balances: List<ParticipantBalance>): List<DebtTransfer> {
        // Trabajamos con centavos enteros para evitar problemas de precisión float
        val credits = ArrayDeque<Pair<ParticipantBalance, Long>>()  // balance > 0 (les deben)
        val debts = ArrayDeque<Pair<ParticipantBalance, Long>>()    // balance < 0 (deben)

        balances.forEach { b ->
            val cents = (b.netBalance * 100).toLong()
            when {
                cents > 0 -> credits.addLast(b to cents)
                cents < 0 -> debts.addLast(b to -cents)  // guardamos como positivo
            }
        }

        // Ordenar mayor primero para convergencia más rápida
        credits.sortByDescending { it.second }
        debts.sortByDescending { it.second }

        val transfers = mutableListOf<DebtTransfer>()

        while (debts.isNotEmpty() && credits.isNotEmpty()) {
            val (debtor, debtCents) = debts.removeFirst()
            val (creditor, creditCents) = credits.removeFirst()

            val transferCents = min(debtCents, creditCents)
            transfers.add(
                DebtTransfer(
                    fromParticipantId = debtor.participantId,
                    fromName = debtor.participantName,
                    toParticipantId = creditor.participantId,
                    toName = creditor.participantName,
                    amount = transferCents / 100.0
                )
            )

            val remainingDebt = debtCents - transferCents
            val remainingCredit = creditCents - transferCents

            if (remainingDebt > 0) debts.addFirst(debtor to remainingDebt)
            if (remainingCredit > 0) credits.addFirst(creditor to remainingCredit)
        }

        return transfers
    }

    private suspend fun ensureSettlementCategory(groupId: Long): Category {
        categoryRepository.getForGroup(groupId)
            .firstOrNull { it.name.equals(SETTLEMENT_CATEGORY_NAME, ignoreCase = true) }
            ?.let { return it }

        return categoryRepository.create(
            Category(
                groupId = groupId,
                name = SETTLEMENT_CATEGORY_NAME,
                icon = SETTLEMENT_CATEGORY_ICON,
                color = SETTLEMENT_CATEGORY_COLOR
            )
        )
    }

    private suspend fun createSettlementSpend(settlement: Settlement, categoryId: Long) {
        val spend = Spend(
            groupId = settlement.groupId,
            concept = SETTLEMENT_SPEND_CONCEPT,
            amount = settlement.amount,
            date = settlement.date,
            payerId = settlement.fromParticipantId,
            categoryId = categoryId,
            splitType = SplitType.CUSTOM,
            notes = buildSettlementSpendNotes(settlement)
        )

        val shares = listOf(
            SpendShare(
                spendId = 0,
                participantId = settlement.toParticipantId,
                amount = settlement.amount
            )
        )

        spendRepository.create(spend, shares)
    }

    private fun buildSettlementSpendNotes(settlement: Settlement): String = buildString {
        append(SETTLEMENT_NOTE_PREFIX)
        append(settlement.id)
        if (settlement.notes.isNotBlank()) {
            append("|")
            append(settlement.notes.trim())
        }
    }

    private fun extractMirroredSettlementId(notes: String): Long? {
        if (!notes.startsWith(SETTLEMENT_NOTE_PREFIX)) return null
        return notes
            .removePrefix(SETTLEMENT_NOTE_PREFIX)
            .substringBefore("|")
            .toLongOrNull()
    }

    private fun Double.roundMoney(): Double =
        (this * 100).roundToLong() / 100.0
}

