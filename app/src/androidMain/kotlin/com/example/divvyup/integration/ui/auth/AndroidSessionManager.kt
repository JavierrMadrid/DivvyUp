package com.example.divvyup.integration.ui.auth

import android.content.Context
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json
import androidx.core.content.edit

/**
 * Persiste la sesión de Supabase en SharedPreferences para que sobreviva al cierre de la app.
 * Sin esta clase, supabase-kt usa MemorySessionManager y la sesión se pierde al matar el proceso.
 */
class AndroidSessionManager(context: Context) : SessionManager {

    private val prefs = context.getSharedPreferences("divvyup_auth", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override suspend fun loadSession(): UserSession? {
        val str = prefs.getString("session", null) ?: return null
        return try {
            json.decodeFromString<UserSession>(str)
        } catch (e: Exception) {
            println("DEBUG AndroidSessionManager: sesión corrupta, eliminando — ${e.message}")
            prefs.edit { remove("session") }
            null
        }
    }

    override suspend fun saveSession(session: UserSession) {
        prefs.edit { putString("session", json.encodeToString(session)) }
    }

    override suspend fun deleteSession() {
        prefs.edit { remove("session") }
    }
}

