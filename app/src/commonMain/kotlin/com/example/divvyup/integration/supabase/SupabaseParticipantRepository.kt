package com.example.divvyup.integration.supabase

import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.repository.ParticipantRepository
import com.example.divvyup.integration.supabase.dto.ParticipantDto
import com.example.divvyup.integration.supabase.dto.toDomain
import com.example.divvyup.integration.supabase.dto.toDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseParticipantRepository(private val postgrest: Postgrest) : ParticipantRepository {

    override suspend fun getByGroup(groupId: Long): List<Participant> = try {
        postgrest.from("participants")
            .select { filter { eq("group_id", groupId) } }
            .decodeList<ParticipantDto>()
            .map { it.toDomain() }
    } catch (e: Exception) {
        throw Exception("Error al obtener participantes: ${e.message}", e)
    }

    override suspend fun create(participant: Participant): Participant = try {
        postgrest.from("participants")
            .insert(participant.toDto()) { select() }
            .decodeSingle<ParticipantDto>()
            .toDomain()
    } catch (e: Exception) {
        throw Exception("Error al añadir participante: ${e.message}", e)
    }

    override suspend fun update(participant: Participant): Participant = try {
        postgrest.from("participants")
            .update(participant.toDto()) {
                select()
                filter { eq("id", participant.id) }
            }
            .decodeSingle<ParticipantDto>()
            .toDomain()
    } catch (e: Exception) {
        throw Exception("Error al actualizar participante: ${e.message}", e)
    }

    override suspend fun delete(id: Long) = try {
        postgrest.from("participants")
            .delete { filter { eq("id", id) } }
        Unit
    } catch (e: Exception) {
        throw Exception("Error al eliminar participante: ${e.message}", e)
    }
}

