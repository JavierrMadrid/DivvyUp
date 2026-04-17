package com.example.divvyup.integration.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential


/**
 * Implementación Android de [GoogleSignInHandler] usando Credential Manager.
 *
 * Usa filterByAuthorizedAccounts=false para mostrar SIEMPRE un único selector
 * con todas las cuentas Google del dispositivo, evitando el doble diálogo.
 *
 * @param context     Activity context — necesario para anclar el selector.
 * @param webClientId Web Client ID del proyecto OAuth (Supabase Auth > Google provider).
 */
class AndroidGoogleSignInHandler(
    private val context: Context,
    private val webClientId: String
) : GoogleSignInHandler {

    override suspend fun getGoogleIdToken(): String? {
        if (webClientId.isBlank()) {
            throw Exception("GOOGLE_WEB_CLIENT_ID no configurado en local.properties")
        }

        val credentialManager = CredentialManager.create(context)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)   // siempre muestra todas las cuentas
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
            println("DEBUG AndroidGoogleSignInHandler: idToken obtenido OK")
            credential.idToken
        } catch (_: GetCredentialCancellationException) {
            println("DEBUG AndroidGoogleSignInHandler: usuario canceló")
            null
        } catch (e: GetCredentialException) {
            println("DEBUG AndroidGoogleSignInHandler: GetCredentialException — ${e::class.simpleName}: ${e.message}")
            // Lanzar CredentialUnavailableException especial para que el ViewModel
            // pueda distinguir "no disponible" (fallback a OAuth) de un error real.
            throw CredentialUnavailableException("Credential Manager no disponible: ${e.message}", e)
        } catch (e: Exception) {
            println("DEBUG AndroidGoogleSignInHandler: error inesperado — ${e::class.simpleName}: ${e.message}")
            throw Exception("Error inesperado en Google Sign-In: ${e.message}", e)
        }
    }
}
