package com.example.divvyup.application

import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.domain.repository.ParticipantRepository
import com.example.divvyup.domain.repository.SpendRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SpendServiceTest {

    @Test
    fun updateEqualSpend_preserva_enlace_de_liquidacion_en_notes() = runTest {
        val existing = Spend(
            id = 100,
            groupId = 10,
            concept = "Liquidación",
            amount = 10.0,
            payerId = 2,
            splitType = SplitType.CUSTOM,
            notes = "__settlement_id:500|nota original",
            date = Instant.parse("2026-04-01T12:00:00Z")
        )
        val repo = RecordingSpendRepository(existing)
        val service = SpendService(
            spendRepository = repo,
            participantRepository = EmptyParticipantRepository()
        )

        service.updateEqualSpend(
            existing = existing,
            concept = "Liquidación",
            amount = 12.0,
            payerId = 2,
            participantIds = listOf(1, 2)
        )

        assertEquals("__settlement_id:500|nota original", repo.lastUpdatedSpend?.notes)
    }

    @Test
    fun updateEqualSpend_preserva_notes_normales_si_no_se_editan() = runTest {
        val existing = Spend(
            id = 101,
            groupId = 10,
            concept = "Cena",
            amount = 20.0,
            payerId = 1,
            splitType = SplitType.EQUAL,
            notes = "nota privada",
            date = Instant.parse("2026-04-01T10:00:00Z")
        )
        val repo = RecordingSpendRepository(existing)
        val service = SpendService(
            spendRepository = repo,
            participantRepository = EmptyParticipantRepository()
        )

        service.updateEqualSpend(
            existing = existing,
            concept = "Cena",
            amount = 22.0,
            payerId = 1,
            participantIds = listOf(1, 2)
        )

        assertEquals("nota privada", repo.lastUpdatedSpend?.notes)
    }
}

private class RecordingSpendRepository(initialSpend: Spend) : SpendRepository {
    private val spends = mutableMapOf(initialSpend.id to initialSpend)
    var lastUpdatedSpend: Spend? = null

    override suspend fun getByGroup(groupId: Long): List<Spend> =
        spends.values.filter { it.groupId == groupId }

    override suspend fun getSharesBySpend(spendId: Long): List<SpendShare> = emptyList()

    override suspend fun getSharesByParticipant(participantId: Long): List<SpendShare> = emptyList()

    override suspend fun create(spend: Spend, shares: List<SpendShare>): Spend = error("No usado")

    override suspend fun update(spend: Spend, shares: List<SpendShare>): Spend {
        lastUpdatedSpend = spend
        spends[spend.id] = spend
        return spend
    }

    override suspend fun delete(id: Long) = error("No usado")

    override suspend fun deleteAll(ids: List<Long>) = error("No usado")
}

private class EmptyParticipantRepository : ParticipantRepository {
    override suspend fun getByGroup(groupId: Long): List<Participant> = emptyList()

    override suspend fun create(participant: Participant): Participant = error("No usado")

    override suspend fun update(participant: Participant): Participant = error("No usado")

    override suspend fun delete(id: Long) = error("No usado")
}

private fun runTest(block: suspend () -> Unit) {
    runBlocking { block() }
}

