package com.example.divvyup.application

import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.domain.repository.CategoryRepository
import com.example.divvyup.domain.repository.ParticipantRepository
import com.example.divvyup.domain.repository.SettlementRepository
import com.example.divvyup.domain.repository.SpendRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Instant

class SettlementServiceTest {

    @Test
    fun getBalances_aplica_liquidaciones_antiguas_sin_duplicar_la_deuda() = runTest {
        val participants = listOf(
            Participant(id = 1, groupId = 10, name = "Ana"),
            Participant(id = 2, groupId = 10, name = "Luis")
        )
        val spends = mutableListOf(
            Spend(
                id = 100,
                groupId = 10,
                concept = "Cena",
                amount = 20.0,
                payerId = 1,
                splitType = SplitType.EQUAL,
                date = Instant.parse("2026-04-01T10:00:00Z")
            )
        )
        val sharesBySpend = mutableMapOf(
            100L to listOf(
                SpendShare(spendId = 100, participantId = 1, amount = 10.0),
                SpendShare(spendId = 100, participantId = 2, amount = 10.0)
            )
        )
        val settlements = mutableListOf(
            Settlement(
                id = 500,
                groupId = 10,
                fromParticipantId = 2,
                toParticipantId = 1,
                amount = 10.0,
                date = Instant.parse("2026-04-01T12:00:00Z")
            )
        )

        val service = SettlementService(
            settlementRepository = FakeSettlementRepository(settlements),
            spendRepository = FakeSpendRepository(spends, sharesBySpend),
            participantRepository = FakeParticipantRepository(participants),
            categoryRepository = FakeCategoryRepository()
        )

        val balances = service.getBalances(10).sortedBy { it.participantId }

        assertEquals(0.0, balances[0].netBalance)
        assertEquals(0.0, balances[1].netBalance)
    }

    @Test
    fun createSettlement_crea_gasto_espejo_y_categoria_liquidacion() = runTest {
        val participants = listOf(
            Participant(id = 1, groupId = 10, name = "Ana"),
            Participant(id = 2, groupId = 10, name = "Luis")
        )
        val spends = mutableListOf(
            Spend(
                id = 100,
                groupId = 10,
                concept = "Cena",
                amount = 20.0,
                payerId = 1,
                splitType = SplitType.EQUAL,
                date = Instant.parse("2026-04-01T10:00:00Z")
            )
        )
        val sharesBySpend = mutableMapOf(
            100L to listOf(
                SpendShare(spendId = 100, participantId = 1, amount = 10.0),
                SpendShare(spendId = 100, participantId = 2, amount = 10.0)
            )
        )
        val settlements = mutableListOf<Settlement>()
        val categories = mutableListOf<Category>()

        val service = SettlementService(
            settlementRepository = FakeSettlementRepository(settlements),
            spendRepository = FakeSpendRepository(spends, sharesBySpend),
            participantRepository = FakeParticipantRepository(participants),
            categoryRepository = FakeCategoryRepository(categories)
        )

        val created = service.createSettlement(
            groupId = 10,
            fromParticipantId = 2,
            toParticipantId = 1,
            amount = 10.0,
            date = Instant.parse("2026-04-01T12:00:00Z")
        )

        assertEquals(1, settlements.size)
        assertEquals(2, spends.size)
        assertEquals(1, categories.size)
        assertEquals("Liquidación", categories.first().name)

        val settlementSpend = spends.last()
        assertEquals("Liquidación", settlementSpend.concept)
        assertEquals(2L, settlementSpend.payerId)
        assertEquals(created.amount, settlementSpend.amount)
        assertEquals(categories.first().id, settlementSpend.categoryId)
        assertNotNull(settlementSpend.notes)
        assertEquals(listOf(SpendShare(spendId = settlementSpend.id, participantId = 1, amount = 10.0)), sharesBySpend[settlementSpend.id])

        val balances = service.getBalances(10).sortedBy { it.participantId }
        assertEquals(0.0, balances[0].netBalance)
        assertEquals(0.0, balances[1].netBalance)
    }

    @Test
    fun deleteSettlement_elimina_tambien_el_gasto_espejo() = runTest {
        val participants = listOf(
            Participant(id = 1, groupId = 10, name = "Ana"),
            Participant(id = 2, groupId = 10, name = "Luis")
        )
        val spends = mutableListOf(
            Spend(
                id = 100,
                groupId = 10,
                concept = "Liquidación",
                amount = 10.0,
                payerId = 2,
                splitType = SplitType.CUSTOM,
                notes = "__settlement_id:500",
                date = Instant.parse("2026-04-01T12:00:00Z")
            )
        )
        val sharesBySpend = mutableMapOf(
            100L to listOf(
                SpendShare(spendId = 100, participantId = 1, amount = 10.0)
            )
        )
        val settlements = mutableListOf(
            Settlement(
                id = 500,
                groupId = 10,
                fromParticipantId = 2,
                toParticipantId = 1,
                amount = 10.0,
                date = Instant.parse("2026-04-01T12:00:00Z")
            )
        )

        val service = SettlementService(
            settlementRepository = FakeSettlementRepository(settlements),
            spendRepository = FakeSpendRepository(spends, sharesBySpend),
            participantRepository = FakeParticipantRepository(participants),
            categoryRepository = FakeCategoryRepository()
        )

        service.deleteSettlement(groupId = 10, id = 500)

        assertEquals(0, settlements.size)
        assertEquals(0, spends.size)
        assertEquals(0, sharesBySpend.size)
    }
}

private class FakeSettlementRepository(
    private val settlements: MutableList<Settlement>
) : SettlementRepository {
    private var nextId = (settlements.maxOfOrNull { it.id } ?: 0L) + 1L

    override suspend fun getByGroup(groupId: Long): List<Settlement> =
        settlements.filter { it.groupId == groupId }

    override suspend fun create(settlement: Settlement): Settlement {
        val created = settlement.copy(id = nextId++)
        settlements += created
        return created
    }

    override suspend fun delete(id: Long) {
        settlements.removeAll { it.id == id }
    }

    override suspend fun getBalances(groupId: Long) = error("No debería usarse en este test")
}

private class FakeSpendRepository(
    private val spends: MutableList<Spend>,
    private val sharesBySpend: MutableMap<Long, List<SpendShare>>
) : SpendRepository {
    private var nextId = (spends.maxOfOrNull { it.id } ?: 0L) + 1L

    override suspend fun getByGroup(groupId: Long): List<Spend> =
        spends.filter { it.groupId == groupId }

    override suspend fun getSharesBySpend(spendId: Long): List<SpendShare> =
        sharesBySpend[spendId].orEmpty()

    override suspend fun getSharesByParticipant(participantId: Long): List<SpendShare> =
        sharesBySpend.values.flatten().filter { it.participantId == participantId }

    override suspend fun create(spend: Spend, shares: List<SpendShare>): Spend {
        val created = spend.copy(id = nextId++)
        spends += created
        sharesBySpend[created.id] = shares.map { it.copy(spendId = created.id) }
        return created
    }

    override suspend fun update(spend: Spend, shares: List<SpendShare>): Spend = error("No usado")

    override suspend fun delete(id: Long) {
        spends.removeAll { it.id == id }
        sharesBySpend.remove(id)
    }

    override suspend fun deleteAll(ids: List<Long>) {
        val idSet = ids.toSet()
        spends.removeAll { it.id in idSet }
        idSet.forEach { sharesBySpend.remove(it) }
    }
}

private class FakeParticipantRepository(
    private val participants: List<Participant>
) : ParticipantRepository {
    override suspend fun getByGroup(groupId: Long): List<Participant> =
        participants.filter { it.groupId == groupId }

    override suspend fun create(participant: Participant): Participant = error("No usado")
    override suspend fun update(participant: Participant): Participant = error("No usado")
    override suspend fun delete(id: Long) = error("No usado")
}

private class FakeCategoryRepository(
    private val categories: MutableList<Category> = mutableListOf()
) : CategoryRepository {
    private var nextId = (categories.maxOfOrNull { it.id } ?: 0L) + 1L

    override suspend fun getForGroup(groupId: Long): List<Category> =
        categories.filter { it.groupId == null || it.groupId == groupId }

    override suspend fun create(category: Category): Category {
        val created = category.copy(id = nextId++)
        categories += created
        return created
    }

    override suspend fun delete(id: Long) = error("No usado")
}

private fun runTest(block: suspend () -> Unit) {
    runBlocking { block() }
}

