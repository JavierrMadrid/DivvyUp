package com.example.divvyup.domain.repository

interface ParticipantUserLinkRepository {
    suspend fun findParticipantIdByGroupAndUser(groupId: Long, userId: String): Long?
    suspend fun assignUserToParticipant(groupId: Long, participantId: Long, userId: String)
    /** Elimina el vínculo de un usuario en un grupo (usado al reasignar por el propietario). */
    suspend fun removeUserLink(groupId: Long, userId: String)
    /**
     * Migra todos los vínculos del usuario anónimo [oldUserId] al usuario registrado [newUserId].
     * Se llama después de que un usuario anónimo inicia sesión en una cuenta existente.
     * Para grupos en los que el nuevo usuario ya tiene vínculo, el vínculo anónimo se descarta.
     */
    suspend fun migrateAnonymousLinks(oldUserId: String, newUserId: String)
}
