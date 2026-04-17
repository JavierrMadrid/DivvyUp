package com.example.divvyup.integration.supabase

import com.example.divvyup.domain.model.InviteToken
import com.example.divvyup.domain.repository.InviteTokenRepository
import com.example.divvyup.integration.supabase.dto.InviteTokenDto
import com.example.divvyup.integration.supabase.dto.toDomain
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseInviteTokenRepository(
    private val postgrest: Postgrest
) : InviteTokenRepository {

    override suspend fun createToken(groupId: Long, createdByUserId: String): InviteToken = try {
        // La BD genera el UUID (gen_random_uuid()) y el expires_at (now() + 7 days) via DEFAULT
        val payload = buildJsonObject {
            put("group_id", groupId)
            put("created_by_user_id", createdByUserId)
        }
        postgrest.from("group_invite_tokens")
            .insert(payload) { select() }
            .decodeSingle<InviteTokenDto>()
            .toDomain()
    } catch (e: Exception) {
        throw Exception("Error al generar el enlace de invitación: ${e.message}", e)
    }

    override suspend fun findValidToken(token: String): InviteToken? = try {
        postgrest.from("group_invite_tokens")
            .select {
                filter {
                    eq("token", token)
                    gt("expires_at", "now()")
                }
            }
            .decodeList<InviteTokenDto>()
            .firstOrNull()
            ?.toDomain()
    } catch (e: Exception) {
        throw Exception("Error al validar el enlace de invitación: ${e.message}", e)
    }
}

