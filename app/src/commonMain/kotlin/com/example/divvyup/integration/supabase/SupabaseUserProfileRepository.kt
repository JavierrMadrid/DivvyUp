package com.example.divvyup.integration.supabase

import com.example.divvyup.domain.model.UserProfile
import com.example.divvyup.domain.repository.UserProfileRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class UserProfileDto(
    @SerialName("user_id")    val userId: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
private data class ParticipantAvatarDto(
    @SerialName("participant_id") val participantId: Long,
    @SerialName("avatar_url")    val avatarUrl: String?
)

private fun UserProfileDto.toDomain() = UserProfile(
    userId = userId, displayName = displayName, avatarUrl = avatarUrl
)

class SupabaseUserProfileRepository(
    private val postgrest: Postgrest
) : UserProfileRepository {

    override suspend fun getProfile(userId: String): UserProfile? = try {
        postgrest.from("user_profiles")
            .select { filter { eq("user_id", userId) } }
            .decodeList<UserProfileDto>()
            .firstOrNull()
            ?.toDomain()
    } catch (e: Exception) {
        throw Exception("Error al obtener perfil: ${e.message}", e)
    }

    override suspend fun upsertProfile(profile: UserProfile): UserProfile = try {
        val dto = UserProfileDto(
            userId = profile.userId,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl
        )
        postgrest.from("user_profiles")
            .upsert(dto) { select() }
            .decodeSingle<UserProfileDto>()
            .toDomain()
    } catch (e: Exception) {
        throw Exception("Error al guardar perfil: ${e.message}", e)
    }

    /**
     * Devuelve mapa participantId → avatarUrl para los participantes vinculados a usuarios
     * que tienen perfil con avatar en el grupo dado.
     * Usa JOIN entre participant_user_links y user_profiles.
     */
    override suspend fun getAvatarUrlsForGroup(groupId: Long): Map<Long, String> = try {
        @Serializable
        data class LinkDto(
            @SerialName("participant_id") val participantId: Long,
            @SerialName("user_id") val userId: String
        )

        val links = postgrest.from("participant_user_links")
            .select { filter { eq("group_id", groupId) } }
            .decodeList<LinkDto>()

        if (links.isEmpty()) return emptyMap()

        val userIds = links.map { it.userId }
        val profiles = postgrest.from("user_profiles")
            .select { filter { isIn("user_id", userIds) } }
            .decodeList<UserProfileDto>()
            .filter { !it.avatarUrl.isNullOrBlank() }
            .associateBy { it.userId }

        links.mapNotNull { link ->
            val url = profiles[link.userId]?.avatarUrl ?: return@mapNotNull null
            link.participantId to url
        }.toMap()
    } catch (e: Exception) {
        // No rompe el flujo si falla: avatares son opcionales
        println("DEBUG SupabaseUserProfileRepository: getAvatarUrlsForGroup error: ${e.message}")
        emptyMap()
    }
}

