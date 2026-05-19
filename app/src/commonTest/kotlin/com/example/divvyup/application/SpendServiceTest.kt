package com.example.divvyup.application

import com.example.divvyup.domain.model.Recurrence
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.domain.repository.SpendRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class SpendServiceTest {

    @Test
    fun updateEqualSpend_preserva_enlace_de_liquidacion_en_notes() = runTest {
        val existing = Spend(
            id = 100, groupId = 10, concept = "Liquidación", amount = 10.0,
            payerId = 2, splitType = SplitType.CUSTOM,
            notes = "__settlement_id:500|nota original",
            date = Instant.parse("2026-04-01T12:00:00Z")
        )
        val repo = RecordingSpendRepository(existing)
        val service = SpendService(spendRepository = repo)
        service.updateEqualSpend(existing = existing, concept = "Liquidación", amount = 12.0,
            payerId = 2, participantIds = listOf(1, 2))
        assertEquals("__settlement_id:500|nota original", repo.lastUpdatedSpend?.notes)
    }

    @Test
    fun updateEqualSpend_preserva_notes_normales_si_no_se_editan() = runTest {
        val existing = Spend(
            id = 101, groupId = 10, concept = "Cena", amount = 20.0,
            payerId = 1, splitType = SplitType.EQUAL, notes = "nota privada",
            date = Instant.parse("2026-04-01T10:00:00Z")
        )
        val repo = RecordingSpendRepository(existing)
        val service = SpendService(spendRepository = repo)
        service.updateEqualSpend(existing = existing, concept = "Cena", amount = 22.0,
            payerId = 1, participantIds = listOf(1, 2))
        assertEquals("nota privada", repo.lastUpdatedSpend?.notes)
    }

    // ── Gastos recurrentes ──────────────────────────────────────────────────

    @Test
    fun advanceNextDue_semanal_suma_7_dias() {
        val service = SpendService(spendRepository = RecordingSpendRepository())
        val base = Instant.parse("2026-03-01T12:00:00Z")
        val next = service.advanceNextDue(base, Recurrence.WEEKLY)
        assertEquals(Instant.parse("2026-03-08T12:00:00Z"), next)
    }

    @Test
    fun materialize_crea_ocurrencia_cuando_nextDue_ya_paso() = runTest {
        val root = Spend(
            id = 1L, groupId = 10, concept = "Alquiler", amount = 600.0,
            payerId = 1, splitType = SplitType.EQUAL,
            recurrence = Recurrence.MONTHLY,
            recurrenceNextDue = Instant.parse("2026-04-01T12:00:00Z"),
            date = Instant.parse("2026-03-01T12:00:00Z")
        )
        val repo = MaterializeTestRepository(root)
        val service = SpendService(repo)
        val now = Instant.parse("2026-05-01T12:00:00Z")

        val generated = service.materializeRecurringSpends(groupId = 10, now = now)

        assertTrue(generated >= 1, "Se esperaba >= 1 ocurrencia; generadas=$generated")
        val ocurrence = repo.created.firstOrNull()
        assertEquals(1L, ocurrence?.recurrenceParentId)
        assertEquals(Recurrence.NONE, ocurrence?.recurrence)
    }

    @Test
    fun materialize_no_crea_nada_si_nextDue_es_futuro() = runTest {
        val root = Spend(
            id = 2L, groupId = 10, concept = "Netflix", amount = 15.0,
            payerId = 1, splitType = SplitType.EQUAL,
            recurrence = Recurrence.MONTHLY,
            recurrenceNextDue = Instant.parse("2026-06-01T12:00:00Z"),
            date = Instant.parse("2026-05-01T12:00:00Z")
        )
        val repo = MaterializeTestRepository(root)
        val service = SpendService(repo)
        val now = Instant.parse("2026-05-19T12:00:00Z")

        val generated = service.materializeRecurringSpends(groupId = 10, now = now)

        assertEquals(0, generated)
        assertTrue(repo.created.isEmpty())
    }
}

// ── Fakes ────────────────────────────────────────────────────────────────────

private class RecordingSpendRepository(vararg initialSpends: Spend) : SpendRepository {
    private val spends = initialSpends.associateBy { it.id }.toMutableMap()
    var lastUpdatedSpend: Spend? = null

    override suspend fun getByGroup(groupId: Long) = spends.values.filter { it.groupId == groupId }
    override suspend fun getSharesBySpend(spendId: Long) = emptyList<SpendShare>()
    override suspend fun getSharesByGroup(groupId: Long) = emptyList<SpendShare>()
    override suspend fun getSharesByParticipant(participantId: Long) = emptyList<SpendShare>()
    override suspend fun create(spend: Spend, shares: List<SpendShare>): Spend = error("No usado")

    override suspend fun update(spend: Spend, shares: List<SpendShare>): Spend {
        lastUpdatedSpend = spend; spends[spend.id] = spend; return spend
    }

    override suspend fun delete(id: Long) = error("No usado")
    override suspend fun deleteAll(ids: List<Long>) = error("No usado")
    override suspend fun getRecurringRootsDue(groupId: Long, dueBeforeOrAt: Instant) = emptyList<Spend>()
    override suspend fun updateNextDue(spendId: Long, nextDue: Instant) {}
}

private class MaterializeTestRepository(private val root: Spend) : SpendRepository {
    val created = mutableListOf<Spend>()

    override suspend fun getByGroup(groupId: Long) = listOf(root)
    override suspend fun getSharesBySpend(spendId: Long) = emptyList<SpendShare>()
    override suspend fun getSharesByGroup(groupId: Long) = emptyList<SpendShare>()
    override suspend fun getSharesByParticipant(participantId: Long) = emptyList<SpendShare>()

    override suspend fun create(spend: Spend, shares: List<SpendShare>): Spend {
        val persisted = spend.copy(id = created.size + 100L)
        created.add(persisted); return persisted
    }

    override suspend fun update(spend: Spend, shares: List<SpendShare>) = spend
    override suspend fun delete(id: Long) {}
    override suspend fun deleteAll(ids: List<Long>) {}

    override suspend fun getRecurringRootsDue(groupId: Long, dueBeforeOrAt: Instant): List<Spend> =
        listOf(root).filter {
            val due = it.recurrenceNextDue ?: return@filter false
            due <= dueBeforeOrAt && it.recurrence != Recurrence.NONE && it.recurrenceParentId == null
        }

    override suspend fun updateNextDue(spendId: Long, nextDue: Instant) {}
}

private fun runTest(block: suspend () -> Unit) { runBlocking { block() } }
