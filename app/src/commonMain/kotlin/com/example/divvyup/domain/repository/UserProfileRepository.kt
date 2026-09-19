package com.example.divvyup.domain.repository

import com.example.divvyup.domain.model.UserProfile

interface UserProfileRepository {
    suspend fun getProfile(userId: String): UserProfile?
    suspend fun upsertProfile(profile: UserProfile): UserProfile
    /** Devuelve avatarUrl → participantId para todos los usuarios vinculados al grupo. */
    suspend fun getAvatarUrlsForGroup(groupId: Long): Map<Long, String>
}

