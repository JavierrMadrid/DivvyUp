package com.example.divvyup.integration.supabase

import com.example.divvyup.domain.repository.ParticipantUserLinkRepository
import com.example.divvyup.integration.supabase.dto.ParticipantUserLinkDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseParticipantUserLinkRepository(
    private val postgrest: Postgrest
) : ParticipantUserLinkRepository {

    override suspend fun findParticipantIdByGroupAndUser(groupId: Long, userId: String): Long? = try {
        postgrest.from("participant_user_links")
            .select {
                filter {
                    eq("group_id", groupId)
                    eq("user_id", userId)
                }
            }
            .decodeList<ParticipantUserLinkDto>()
            .firstOrNull()
            ?.participantId
    } catch (e: Exception) {
        throw Exception("Error al consultar la asignación del usuario: ${e.message}", e)
    }

    override suspend fun assignUserToParticipant(groupId: Long, participantId: Long, userId: String) {
        try {
            // Eliminar primero cualquier vínculo previo del usuario en este grupo
            // (permite "reasignar" sin violar unique(group_id, user_id))
            postgrest.from("participant_user_links")
                .delete { filter { eq("group_id", groupId); eq("user_id", userId) } }

            // Insertar el nuevo vínculo
            postgrest.from("participant_user_links")
                .insert(
                    ParticipantUserLinkDto(
                        groupId = groupId,
                        participantId = participantId,
                        userId = userId
                    )
                )
        } catch (e: Exception) {
            throw Exception("Error al asignar el usuario al participante: ${e.message}", e)
        }
    }

    override suspend fun removeUserLink(groupId: Long, userId: String) {
        try {
            postgrest.from("participant_user_links")
                .delete {
                    filter {
                        eq("group_id", groupId)
                        eq("user_id", userId)
                    }
                }
        } catch (e: Exception) {
            throw Exception("Error al eliminar la asignación del usuario: ${e.message}", e)
        }
    }

    override suspend fun migrateAnonymousLinks(oldUserId: String, newUserId: String) {
        try {
            // 1. Obtener todos los vínculos del usuario anónimo
            val anonLinks = postgrest.from("participant_user_links")
                .select { filter { eq("user_id", oldUserId) } }
                .decodeList<ParticipantUserLinkDto>()

            if (anonLinks.isEmpty()) return

            // 2. Obtener los group_ids donde el usuario real ya tiene vínculo (no sobrescribir)
            val realUserGroupIds = postgrest.from("participant_user_links")
                .select { filter { eq("user_id", newUserId) } }
                .decodeList<ParticipantUserLinkDto>()
                .map { it.groupId }
                .toSet()

            // 3. Para cada vínculo anónimo sin conflicto, crear uno con el UID real
            anonLinks
                .filter { it.groupId !in realUserGroupIds }
                .forEach { link ->
                    try {
                        postgrest.from("participant_user_links")
                            .insert(
                                ParticipantUserLinkDto(
                                    groupId = link.groupId,
                                    participantId = link.participantId,
                                    userId = newUserId
                                )
                            )
                    } catch (_: Exception) { /* conflicto — ignorar */ }
                }

            // 4. Eliminar todos los vínculos anónimos
            postgrest.from("participant_user_links")
                .delete { filter { eq("user_id", oldUserId) } }

        } catch (e: Exception) {
            throw Exception("Error al migrar vínculos de usuario anónimo: ${e.message}", e)
        }
    }
}
