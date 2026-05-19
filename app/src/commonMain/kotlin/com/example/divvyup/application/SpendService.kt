@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.example.divvyup.application

import com.example.divvyup.domain.model.Recurrence
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.domain.repository.SpendRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant


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

    suspend fun getSharesByGroup(groupId: Long): List<SpendShare> =
        spendRepository.getSharesByGroup(groupId)

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

    // --- Gastos recurrentes ---

    /**
     * Genera automáticamente las ocurrencias vencidas de todos los gastos recurrentes del grupo.
     *
     * Algoritmo:
     * 1. Consulta gastos raíz con [recurrenceNextDue] ≤ [now].
     * 2. Para cada raíz, crea una ocurrencia (gasto normal, recurrence=NONE, recurrenceParentId=root.id)
     *    con fecha = recurrenceNextDue.
     * 3. Clona las shares del raíz para la nueva ocurrencia.
     * 4. Avanza el nextDue del raíz (semanal +7d, mensual +1 mes).
     * 5. Si el nuevo nextDue también está vencido, repite (sin límite de iteraciones pero máx 24
     *    para evitar bucles infinitos en grupos sin actividad durante mucho tiempo).
     *
     * La restricción SQL UNIQUE(recurrence_parent_id, recurrence_next_due) en BD garantiza
     * idempotencia ante múltiples llamadas concurrentes.
     */
    suspend fun materializeRecurringSpends(groupId: Long, now: Instant = Clock.System.now()): Int {
        val roots = try {
            spendRepository.getRecurringRootsDue(groupId, now)
        } catch (e: Exception) {
            println("DEBUG SpendService: materializeRecurringSpends — getRecurringRootsDue falló: ${e.message}")
            return 0
        }

        var created = 0

        for (root in roots) {
            var nextDue = root.recurrenceNextDue ?: continue
            var iterations = 0

            while (nextDue <= now && iterations < 24) {
                iterations++

                // Obtener shares del raíz para clonarlas
                val originalShares = try {
                    spendRepository.getSharesBySpend(root.id)
                } catch (_: Exception) {
                    emptyList()
                }

                // Construir ocurrencia
                val occurrence = root.copy(
                    id = 0,
                    date = nextDue,
                    recurrence = Recurrence.NONE,
                    recurrenceParentId = root.id,
                    recurrenceNextDue = null,
                    createdAt = now
                )

                val occurrenceShares = originalShares.map { it.copy(id = 0, spendId = 0) }

                try {
                    spendRepository.create(occurrence, occurrenceShares)
                    created++
                } catch (e: Exception) {
                    // Si viola la restricción única, ya existe → no es error, simplemente avanzar
                    println("DEBUG SpendService: ocurrencia ya existía para root=${root.id} due=$nextDue (${e.message})")
                }

                // Calcular el próximo vencimiento
                nextDue = advanceNextDue(nextDue, root.recurrence)
            }

            // Actualizar el nextDue del raíz al siguiente vencimiento futuro
            try {
                spendRepository.updateNextDue(root.id, nextDue)
            } catch (e: Exception) {
                println("DEBUG SpendService: updateNextDue falló para root=${root.id}: ${e.message}")
            }
        }

        return created
    }

    /**
     * Inicializa [recurrenceNextDue] en un gasto raíz recién creado.
     * Debe llamarse inmediatamente después de crear el gasto.
     */
    suspend fun initializeNextDue(spend: Spend): Instant {
        val nextDue = advanceNextDue(spend.date, spend.recurrence)
        try {
            spendRepository.updateNextDue(spend.id, nextDue)
        } catch (e: Exception) {
            println("DEBUG SpendService: initializeNextDue falló para spend=${spend.id}: ${e.message}")
        }
        return nextDue
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

    /**
     * Calcula la siguiente fecha de vencimiento dado un instante y una frecuencia.
     * Usa zona horaria local del sistema para hacer el avance de mes/semana correctamente.
     */
    internal fun advanceNextDue(from: Instant, recurrence: Recurrence): Instant {
        val tz = TimeZone.currentSystemDefault()
        return when (recurrence) {
            Recurrence.WEEKLY  -> from + 7.days
            Recurrence.MONTHLY -> {
                val localDt = from.toLocalDateTime(tz)
                val nextLocalDate = localDt.date.plus(DatePeriod(months = 1))
                // Reconstruir el Instant preservando la hora del día original
                LocalDateTime(
                    date = nextLocalDate,
                    time = localDt.time
                ).toInstant(tz)
            }
            Recurrence.DAILY   -> from + 1.days
            Recurrence.NONE    -> from   // no debería llegar aquí
        }
    }
}
