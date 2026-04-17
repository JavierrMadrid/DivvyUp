package com.example.divvyup.integration.ui.auth

/**
 * Se lanza cuando el selector nativo de Google no está disponible
 * (p.ej. configuración de Google Cloud incompleta, SHA-1 no registrado).
 * El ViewModel la captura para hacer fallback al flujo OAuth por navegador.
 */
class CredentialUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Contrato multiplataforma para el inicio de sesión nativo con Google.
 *
 * En Android se implementa con Credential Manager (sin navegador).
 * En iOS (y otras plataformas) se puede dejar como no-op.
 *
 * Se pasa al [AuthViewModel] como dependencia opcional;
 * si es null, el ViewModel cae al flujo OAuth estándar.
 */
fun interface GoogleSignInHandler {
    /**
     * Lanza el selector de cuenta de Google nativo y devuelve el idToken
     * si el usuario confirma, o null si cancela / hay error.
     */
    suspend fun getGoogleIdToken(): String?
}
