package com.example.divvyup.application

import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.InviteToken
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.repository.GroupRepository
import com.example.divvyup.domain.repository.InviteTokenRepository
import com.example.divvyup.domain.repository.ParticipantRepository
import com.example.divvyup.domain.repository.ParticipantUserLinkRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class InvitationServiceTest {

    // ── assignUserToParticipant ───────────────────────────────────────────────

    @Test
    fun assignUserToParticipant_guarda_vinculo_si_es_valido() = runTest {
        val service = buildService()

        service.assignUserToParticipant(groupId = 1, participantId = 10, userId = "user-1")

        val assigned = service.getAssignedParticipantId(1, "user-1")
        assertEquals(10L, assigned)
    }

    @Test
    fun assignUserToParticipant_es_idempotente_al_asignar_mismo_participante() = runTest {
        val linkRepo = InvitationFakeParticipantUserLinkRepository(
            initial = mutableMapOf((1L to "user-1") to 10L)
        )
        val service = buildService(linkRepo = linkRepo)

        // No debe lanzar excepción
        service.assignUserToParticipant(groupId = 1, participantId = 10, userId = "user-1")
    }

    @Test
    fun assignUserToParticipant_falla_si_usuario_no_propietario_intenta_reasignar() = runTest {
        val linkRepo = InvitationFakeParticipantUserLinkRepository(
            initial = mutableMapOf((1L to "user-invitado") to 11L)
        )
        // grupo sin propietario asignado (o propietario diferente)
        val groupRepo = InvitationFakeGroupRepository(ownerUserId = "user-owner")
        val service = buildService(groupRepo = groupRepo, linkRepo = linkRepo)

        val error = assertFailsWith<Exception> {
            service.assignUserToParticipant(groupId = 1, participantId = 10, userId = "user-invitado")
        }
        assertEquals("Ya estás asignado a otro participante en este grupo", error.message)
    }

    @Test
    fun assignUserToParticipant_permite_reasignar_al_propietario() = runTest {
        val linkRepo = InvitationFakeParticipantUserLinkRepository(
            initial = mutableMapOf((1L to "user-owner") to 11L)
        )
        val groupRepo = InvitationFakeGroupRepository(ownerUserId = "user-owner")
        val service = buildService(groupRepo = groupRepo, linkRepo = linkRepo)

        // El propietario puede reasignarse
        service.assignUserToParticipant(groupId = 1, participantId = 10, userId = "user-owner")

        assertEquals(10L, service.getAssignedParticipantId(1, "user-owner"))
    }

    // ── getJoinInfo ───────────────────────────────────────────────────────────

    @Test
    fun getJoinInfo_falla_si_no_hay_participantes() = runTest {
        val service = buildService(participants = emptyList())

        val error = assertFailsWith<Exception> { service.getJoinInfo(groupId = 1) }
        assertEquals("Este grupo no tiene participantes disponibles", error.message)
    }

    // ── getJoinInfoByToken ────────────────────────────────────────────────────

    @Test
    fun getJoinInfoByToken_retorna_info_con_token_valido() = runTest {
        val validToken = InviteToken(
            token = "abc-token",
            groupId = 1,
            createdByUserId = "user-owner",
            expiresAt = Clock.System.now().plus(1.days)
        )
        val tokenRepo = InvitationFakeInviteTokenRepository(validToken)
        val service = buildService(tokenRepo = tokenRepo)

        val info = service.getJoinInfoByToken("abc-token")
        assertNotNull(info)
        assertEquals("Viaje", info.group.name)
    }

    @Test
    fun getJoinInfoByToken_falla_con_token_inexistente() = runTest {
        val service = buildService()

        val error = assertFailsWith<Exception> { service.getJoinInfoByToken("token-caducado") }
        assertEquals("El enlace de invitación ha caducado o no es válido", error.message)
    }

    // ── generateInviteLink ────────────────────────────────────────────────────

    @Test
    fun generateInviteLink_falla_si_no_es_el_propietario() = runTest {
        val groupRepo = InvitationFakeGroupRepository(ownerUserId = "user-owner")
        val service = buildService(groupRepo = groupRepo)

        val error = assertFailsWith<Exception> {
            service.generateInviteLink(groupId = 1, requestingUserId = "otro-usuario")
        }
        assertEquals("Solo el creador del grupo puede generar el enlace de invitación", error.message)
    }

    @Test
    fun generateInviteLink_devuelve_deep_link_con_token() = runTest {
        val groupRepo = InvitationFakeGroupRepository(ownerUserId = "user-owner")
        val tokenRepo = InvitationFakeInviteTokenRepository(null)
        val service = buildService(groupRepo = groupRepo, tokenRepo = tokenRepo)

        val link = service.generateInviteLink(groupId = 1, requestingUserId = "user-owner")
        assertTrue(link.startsWith("divvyup://join?token="), "Enlace inesperado: $link")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildService(
        groupRepo: InvitationFakeGroupRepository = InvitationFakeGroupRepository(),
        participants: List<Participant> = listOf(
            Participant(id = 10, groupId = 1, name = "Ana"),
            Participant(id = 11, groupId = 1, name = "Luis")
        ),
        linkRepo: InvitationFakeParticipantUserLinkRepository = InvitationFakeParticipantUserLinkRepository(),
        tokenRepo: InvitationFakeInviteTokenRepository = InvitationFakeInviteTokenRepository(null)
    ): InvitationService {
        val participantRepo = InvitationFakeParticipantRepository(participants)
        return InvitationService(
            groupRepository               = groupRepo,
            participantRepository         = participantRepo,
            participantUserLinkRepository = linkRepo,
            inviteTokenRepository         = tokenRepo
        )
    }
}

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class InvitationFakeGroupRepository(
    private val ownerUserId: String? = null
) : GroupRepository {
    private val group = Group(
        id = 1,
        name = "Viaje",
        ownerUserId = ownerUserId,
        createdAt = Instant.parse("2026-01-01T00:00:00Z")
    )
    override suspend fun getAll()          = listOf(group)
    override suspend fun getById(id: Long) = group
    override suspend fun create(group: Group) = group
    override suspend fun update(group: Group) = group
    override suspend fun delete(id: Long) = Unit
}

private class InvitationFakeParticipantRepository(
    private val participants: List<Participant>
) : ParticipantRepository {
    override suspend fun getByGroup(groupId: Long) = participants.filter { it.groupId == groupId }
    override suspend fun create(participant: Participant) = participant
    override suspend fun update(participant: Participant) = participant
    override suspend fun delete(id: Long) = Unit
}

private class InvitationFakeParticipantUserLinkRepository(
    initial: MutableMap<Pair<Long, String>, Long> = mutableMapOf()
) : ParticipantUserLinkRepository {
    private val links = initial

    override suspend fun findParticipantIdByGroupAndUser(groupId: Long, userId: String) =
        links[groupId to userId]

    override suspend fun assignUserToParticipant(groupId: Long, participantId: Long, userId: String) {
        links[groupId to userId] = participantId
    }

    override suspend fun removeUserLink(groupId: Long, userId: String) {
        links.remove(groupId to userId)
    }

    override suspend fun migrateAnonymousLinks(oldUserId: String, newUserId: String) {
        val toMigrate = links.entries
            .filter { it.key.second == oldUserId }
            .toList()
        toMigrate.forEach { (key, participantId) ->
            val (groupId, _) = key
            if (!links.containsKey(groupId to newUserId)) {
                links[groupId to newUserId] = participantId
            }
            links.remove(key)
        }
    }
}

private class InvitationFakeInviteTokenRepository(
    private val validToken: InviteToken?
) : InviteTokenRepository {
    private var lastCreatedToken: InviteToken? = null

    override suspend fun createToken(groupId: Long, createdByUserId: String): InviteToken {
        val token = InviteToken(
            token = "generated-uuid-${groupId}",
            groupId = groupId,
            createdByUserId = createdByUserId,
            expiresAt = Clock.System.now().plus(7.days)
        )
        lastCreatedToken = token
        return token
    }

    override suspend fun findValidToken(token: String): InviteToken? =
        validToken?.takeIf { it.token == token }
}

private fun runTest(block: suspend () -> Unit) {
    runBlocking { block() }
}
