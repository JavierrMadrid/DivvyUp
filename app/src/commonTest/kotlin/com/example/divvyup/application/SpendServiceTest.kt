package com.example.divvyup.application

import com.example.divvyup.domain.model.Recurrence
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.domain.repository.SpendCursor
import com.example.divvyup.domain.repository.SpendPage
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

    // ── Paginación (getSpendsPage) ──────────────────────────────────────────

    @Test
    fun getSpendsPage_primera_pagina_sin_repetir_y_con_hasMore() = runTest {
        // 5 gastos: fechas distintas + ids para desempate
        val spends = listOf(
            spend(1, date = "2026-04-01T12:00:00Z"),
            spend(2, date = "2026-04-01T11:00:00Z"),
            spend(3, date = "2026-03-15T12:00:00Z"),
            spend(4, date = "2026-03-10T12:00:00Z"),
            spend(5, date = "2026-03-01T12:00:00Z")
        )
        val repo = RecordingSpendRepository(*spends.toTypedArray())
        val service = SpendService(repo)

        val page1 = service.getSpendsPage(groupId = 10, pageSize = 2, before = null)
        assertEquals(listOf(1L, 2L), page1.items.map { it.id })
        assertTrue(page1.hasMore, "Con 5 gastos y pageSize=2 debe haber más páginas")

        val page2 = service.getSpendsPage(
            groupId = 10, pageSize = 2,
            before = SpendCursor(date = page1.items.last().date, id = page1.items.last().id)
        )
        assertEquals(listOf(3L, 4L), page2.items.map { it.id })
        assertTrue(page2.hasMore)

        val page3 = service.getSpendsPage(
            groupId = 10, pageSize = 2,
            before = SpendCursor(date = page2.items.last().date, id = page2.items.last().id)
        )
        assertEquals(listOf(5L), page3.items.map { it.id })
        assertTrue(!page3.hasMore, "La última página debe tener hasMore=false")

        val all = page1.items + page2.items + page3.items
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), all.map { it.id }, "No debe repetirse ningún gasto entre páginas")
    }

    @Test
    fun getSpendsPage_desempata_misma_fecha_por_id_desc() = runTest {
        val spends = listOf(
            spend(10, date = "2026-04-01T12:00:00Z"),
            spend(11, date = "2026-04-01T12:00:00Z"),
            spend(12, date = "2026-04-01T12:00:00Z")
        )
        val repo = RecordingSpendRepository(*spends.toTypedArray())
        val service = SpendService(repo)

        val page1 = service.getSpendsPage(groupId = 10, pageSize = 2, before = null)
        assertEquals(listOf(12L, 11L), page1.items.map { it.id }, "Orden esperado: id DESC entre gastos de la misma fecha")

        val page2 = service.getSpendsPage(
            groupId = 10, pageSize = 2,
            before = SpendCursor(date = page1.items.last().date, id = page1.items.last().id)
        )
        assertEquals(listOf(10L), page2.items.map { it.id }, "El cursor (date, id) debe saltar el último ya leído")
    }

    private fun spend(id: Long, date: String) = Spend(
        id = id, groupId = 10, concept = "Gasto $id", amount = 10.0,
        payerId = 1, splitType = SplitType.EQUAL,
        date = Instant.parse(date)
    )
}

// ── Fakes ────────────────────────────────────────────────────────────────────

private class RecordingSpendRepository(vararg initialSpends: Spend) : SpendRepository {
    private val spends = initialSpends.associateBy { it.id }.toMutableMap()
    var lastUpdatedSpend: Spend? = null

    override suspend fun getByGroup(groupId: Long) = spends.values.filter { it.groupId == groupId }
    override suspend fun getSpendsPage(groupId: Long, pageSize: Int, before: SpendCursor?): SpendPage {
        val sorted = spends.values.filter { it.groupId == groupId }
            .sortedWith(compareByDescending<Spend> { it.date }.thenByDescending { it.id })
        val startIndex = before?.let { b -> sorted.indexOfFirst { it.id == b.id } + 1 } ?: 0
        val page = sorted.drop(startIndex).take(pageSize)
        return SpendPage(items = page, hasMore = startIndex + page.size < sorted.size)
    }
    override suspend fun getLastSpendDate(groupId: Long) = spends.values.filter { it.groupId == groupId }.maxOfOrNull { it.date }
    override suspend fun getSharesBySpend(spendId: Long) = emptyList<SpendShare>()
    override suspend fun getSharesBySpendIds(spendIds: List<Long>) = emptyList<SpendShare>()
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
    override suspend fun getSpendsPage(groupId: Long, pageSize: Int, before: SpendCursor?): SpendPage =
        SpendPage(items = listOf(root).take(pageSize), hasMore = false)
    override suspend fun getLastSpendDate(groupId: Long) = root.date
    override suspend fun getSharesBySpend(spendId: Long) = emptyList<SpendShare>()
    override suspend fun getSharesBySpendIds(spendIds: List<Long>) = emptyList<SpendShare>()
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
