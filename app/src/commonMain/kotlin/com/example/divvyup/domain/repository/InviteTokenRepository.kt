package com.example.divvyup.domain.repository

import com.example.divvyup.domain.model.InviteToken

interface InviteTokenRepository {
    /** Crea un token nuevo para un grupo (UUID v4 generado en el servidor). */
    suspend fun createToken(groupId: Long, createdByUserId: String): InviteToken
    /** Busca el token y devuelve null si no existe o está caducado. */
    suspend fun findValidToken(token: String): InviteToken?
}

