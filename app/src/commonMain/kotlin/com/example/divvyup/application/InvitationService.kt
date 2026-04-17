package com.example.divvyup.application

import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.repository.GroupRepository
import com.example.divvyup.domain.repository.InviteTokenRepository
import com.example.divvyup.domain.repository.ParticipantRepository
import com.example.divvyup.domain.repository.ParticipantUserLinkRepository

data class GroupJoinInfo(
    val group: Group,
    val participants: List<Participant>
)

class InvitationService(
    private val groupRepository: GroupRepository,
    private val participantRepository: ParticipantRepository,
    private val participantUserLinkRepository: ParticipantUserLinkRepository,
    private val inviteTokenRepository: InviteTokenRepository
) {

    // ── Compartir: el propietario genera un token con TTL 7 días ───────────

    /**
     * Genera (o reutiliza si el propietario ya tiene uno vigente) un enlace de invitación.
     * Solo el propietario del grupo puede generarlo.
     * Devuelve el deep link: `divvyup://join?token=<uuid>`
     */
    suspend fun generateInviteLink(groupId: Long, requestingUserId: String): String {
        require(requestingUserId.isNotBlank()) { "No se ha encontrado la sesión del usuario" }

        val group = groupRepository.getById(groupId)
        if (group.ownerUserId != null && group.ownerUserId != requestingUserId) {
            throw Exception("Solo el creador del grupo puede generar el enlace de invitación")
        }

        val tokenInfo = inviteTokenRepository.createToken(
            groupId = groupId,
            createdByUserId = requestingUserId
        )
        return "divvyup://join?token=${tokenInfo.token}"
    }

    // ── Unirse: valida token y carga participantes ─────────────────────────

    /**
     * Valida el token del deep link y devuelve la información del grupo para mostrar
     * la pantalla de selección de participante.
     */
    suspend fun getJoinInfoByToken(token: String): GroupJoinInfo {
        val tokenInfo = inviteTokenRepository.findValidToken(token)
            ?: throw Exception("El enlace de invitación ha caducado o no es válido")

        return getJoinInfo(tokenInfo.groupId)
    }

    suspend fun getJoinInfo(groupId: Long): GroupJoinInfo {
        val group = groupRepository.getById(groupId)
        val participants = participantRepository.getByGroup(groupId)
        if (participants.isEmpty()) {
            throw Exception("Este grupo no tiene participantes disponibles")
        }
        return GroupJoinInfo(group = group, participants = participants)
    }

    suspend fun getAssignedParticipantId(groupId: Long, userId: String): Long? {
        require(userId.isNotBlank()) { "No se ha encontrado la sesión del usuario" }
        return participantUserLinkRepository.findParticipantIdByGroupAndUser(groupId, userId)
    }

    // ── Asignación: un usuario se vincula a un participante ────────────────

    /**
     * Asigna el usuario autenticado a un participante.
     * - Si el usuario aún no tiene participante en el grupo → asigna libremente.
     * - Si ya tiene uno asignado:
     *     · Si es el mismo participante → no-op (idempotente).
     *     · Si es diferente → solo permitido si el usuario es el propietario del grupo.
     */
    suspend fun assignUserToParticipant(
        groupId: Long,
        participantId: Long,
        userId: String
    ) {
        require(userId.isNotBlank()) { "No se ha encontrado la sesión del usuario" }

        val participants = participantRepository.getByGroup(groupId)
        if (participants.none { it.id == participantId }) {
            throw Exception("El participante seleccionado no pertenece a este grupo")
        }

        val existingParticipantId =
            participantUserLinkRepository.findParticipantIdByGroupAndUser(groupId, userId)

        if (existingParticipantId != null) {
            if (existingParticipantId == participantId) return  // idempotente

            // Reasignación → solo propietario
            val group = groupRepository.getById(groupId)
            if (group.ownerUserId == null || group.ownerUserId != userId) {
                throw Exception("Ya estás asignado a otro participante en este grupo")
            }

            // El propietario reasigna: eliminar vínculo antiguo e insertar nuevo
            participantUserLinkRepository.removeUserLink(groupId = groupId, userId = userId)
        }

        participantUserLinkRepository.assignUserToParticipant(
            groupId = groupId,
            participantId = participantId,
            userId = userId
        )
    }
}
