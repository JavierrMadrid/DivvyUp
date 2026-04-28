package com.example.divvyup.integration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvyup.integration.ui.auth.CredentialUnavailableException
import com.example.divvyup.integration.ui.auth.GoogleSignInHandler
import com.example.divvyup.domain.model.UserProfile
import com.example.divvyup.domain.repository.UserProfileRepository
import com.example.divvyup.integration.supabase.SupabaseStorageService
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

const val OAUTH_REDIRECT_URL = "divvyup://auth-callback"

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isAnonymous: Boolean = false,
    val error: String? = null,
    val registrationPendingConfirmation: Boolean = false,
    val displayName: String = "",
    val userEmail: String = "",
    val isSavingProfile: Boolean = false,
    val profileSavedMessage: String? = null,
    /** URL del avatar del usuario (de user_profiles). */
    val avatarUrl: String? = null
)

class AuthViewModel(
    private val auth: Auth,
    private val googleSignInHandler: GoogleSignInHandler? = null,
    private val onAnonymousMigration: (suspend (String, String) -> Unit)? = null,
    private val userProfileRepository: UserProfileRepository? = null,
    private val storageService: SupabaseStorageService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Esperar a que el gestor de sesión termine de cargar desde almacenamiento antes de
        // evaluar el estado — evita la race condition que creaba una sesión anónima encima
        // de una sesión real ya almacenada.
        viewModelScope.launch {
            auth.sessionStatus.first { it !is SessionStatus.Initializing }
            checkSession()
        }
    }

    fun checkSession() {
        viewModelScope.launch {
            val session = auth.currentSessionOrNull()
            if (session == null) {
                // No hay sesión → crear sesión anónima automáticamente (modo invitado)
                try {
                    auth.signInAnonymously()
                    val newSession = auth.currentSessionOrNull()
                    val isAnon = newSession?.user?.isAnonymous == true
                    _uiState.update {
                        it.copy(isAnonymous = isAnon, isAuthenticated = !isAnon && newSession != null)
                    }
                    println("DEBUG AuthViewModel: sesión anónima creada uid=${newSession?.user?.id}")
                } catch (e: Exception) {
                    println("DEBUG AuthViewModel: signInAnonymously falló (sin red?) — ${e.message}")
                    _uiState.update { it.copy(isAnonymous = false, isAuthenticated = false) }
                }
            } else {
                val isAnon = session.user?.isAnonymous == true
                val displayName = session.user?.userMetadata?.get("display_name")
                    ?.jsonPrimitive?.contentOrNull ?: ""
                val email = session.user?.email ?: ""
                _uiState.update {
                    it.copy(
                        isAuthenticated = !isAnon,
                        isAnonymous = isAnon,
                        displayName = displayName,
                        userEmail = email
                    )
                }
                // Cargar avatar desde user_profiles si hay sesión real
                if (!isAnon) loadAvatarUrl(session.user?.id)
                println("DEBUG AuthViewModel: sesión existente uid=${session.user?.id} isAnon=$isAnon")
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "El correo y la contraseña son obligatorios") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Guardar el UID anónimo ANTES del login para poder migrar sus vínculos
                val anonymousUserId = auth.currentSessionOrNull()
                    ?.takeIf { it.user?.isAnonymous == true }
                    ?.user?.id

                auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }

                val newUserId = auth.currentSessionOrNull()?.user?.id
                // Si había sesión anónima y ahora tenemos un UID diferente → migrar vínculos
                if (anonymousUserId != null && newUserId != null && anonymousUserId != newUserId) {
                    try {
                        onAnonymousMigration?.invoke(anonymousUserId, newUserId)
                        println("DEBUG AuthViewModel: vínculos anónimos migrados $anonymousUserId → $newUserId")
                    } catch (e: Exception) {
                        println("DEBUG AuthViewModel: migración anónima falló (no crítico) — ${e.message}")
                    }
                }

                val session = auth.currentSessionOrNull()
                val displayName = session?.user?.userMetadata?.get("display_name")
                    ?.jsonPrimitive?.contentOrNull ?: ""
                val userEmail = session?.user?.email ?: email.trim()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        isAnonymous = false,
                        displayName = displayName,
                        userEmail = userEmail
                    )
                }
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: login error — ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = traducirErrorAuth(e.message)) }
            }
        }
    }

    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "El correo y la contraseña son obligatorios") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(error = "La contraseña debe tener al menos 6 caracteres") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Guardar el UID anónimo ANTES del registro para poder migrar sus vínculos
                val anonymousUserId = auth.currentSessionOrNull()
                    ?.takeIf { it.user?.isAnonymous == true }
                    ?.user?.id

                // Siempre usar signUpWith — updateUser sobre sesión anónima activa el flujo
                // de "cambio de contraseña" en Supabase y genera el error
                // "New password should be different from the old password".
                auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }

                // Si había sesión anónima, migrar vínculos al nuevo usuario registrado
                val newUserId = auth.currentSessionOrNull()?.user?.id
                if (anonymousUserId != null && newUserId != null && anonymousUserId != newUserId) {
                    try {
                        onAnonymousMigration?.invoke(anonymousUserId, newUserId)
                        println("DEBUG AuthViewModel: vínculos anónimos migrados en registro $anonymousUserId → $newUserId")
                    } catch (e: Exception) {
                        println("DEBUG AuthViewModel: migración anónima en registro falló (no crítico) — ${e.message}")
                    }
                }

                val session = auth.currentSessionOrNull()
                val emailConfirmedAt = session?.user?.emailConfirmedAt
                if (session != null && emailConfirmedAt != null) {
                    // Email ya confirmado (poco habitual en prod, posible en dev sin confirmación)
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true, isAnonymous = false) }
                } else {
                    // Supabase requiere confirmación de correo → cerrar sesión temporal y señalizar
                    try { auth.signOut() } catch (_: Exception) {}
                    _uiState.update {
                        it.copy(isLoading = false, registrationPendingConfirmation = true)
                    }
                }
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: register error — ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = traducirErrorAuth(e.message)) }
            }
        }
    }

    fun startGoogleLogin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val anonymousUserId = auth.currentSessionOrNull()
                    ?.takeIf { it.user?.isAnonymous == true }
                    ?.user?.id

                auth.signInWith(Google, redirectUrl = OAUTH_REDIRECT_URL) {
                    scopes.clear()
                    scopes.add("email")
                    scopes.add("profile")
                    automaticallyOpenUrl = true
                }

                val newUserId = auth.currentSessionOrNull()?.user?.id
                if (anonymousUserId != null && newUserId != null && anonymousUserId != newUserId) {
                    try { onAnonymousMigration?.invoke(anonymousUserId, newUserId) } catch (_: Exception) {}
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: startGoogleLogin error — ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = traducirErrorAuth(e.message)) }
            }
        }
    }

    /**
     * Inicia sesión con Google usando el selector nativo (Credential Manager en Android).
     * No abre el navegador. Requiere que se haya pasado un [GoogleSignInHandler] al constructor.
     * Si no hay handler disponible (iOS), cae al flujo OAuth estándar.
     */
    fun loginWithGoogleNative() {
        val handler = googleSignInHandler
        if (handler == null) {
            startGoogleLogin()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val anonymousUserId = auth.currentSessionOrNull()
                    ?.takeIf { it.user?.isAnonymous == true }
                    ?.user?.id

                val idToken = handler.getGoogleIdToken()
                if (idToken == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                auth.signInWith(IDToken) {
                    this.idToken = idToken
                    this.provider = Google
                }

                val newUserId = auth.currentSessionOrNull()?.user?.id
                if (anonymousUserId != null && newUserId != null && anonymousUserId != newUserId) {
                    try { onAnonymousMigration?.invoke(anonymousUserId, newUserId) } catch (_: Exception) {}
                }

                val session = auth.currentSessionOrNull()
                val displayName = session?.user?.userMetadata?.get("display_name")
                    ?.jsonPrimitive?.contentOrNull ?: ""
                val userEmail = session?.user?.email ?: ""
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        isAnonymous = false,
                        displayName = displayName,
                        userEmail = userEmail
                    )
                }
            } catch (e: Exception) {
                if (e is CredentialUnavailableException) {
                    println("DEBUG AuthViewModel: Credential Manager no disponible, fallback a OAuth — ${e.message}")
                    _uiState.update { it.copy(isLoading = false) }
                    startGoogleLogin()
                } else {
                    println("DEBUG AuthViewModel: loginWithGoogleNative error — ${e.message}")
                    _uiState.update { it.copy(isLoading = false, error = traducirErrorAuth(e.message)) }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signOut()
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: logout error — ${e.message}")
            }
            // Tras cerrar sesión, crear sesión anónima automáticamente (modo invitado)
            try {
                auth.signInAnonymously()
                println("DEBUG AuthViewModel: sesión anónima creada tras logout")
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: signInAnonymously tras logout falló — ${e.message}")
            }
            val session = auth.currentSessionOrNull()
            val isAnon = session?.user?.isAnonymous == true
            _uiState.update {
                it.copy(isLoading = false, isAuthenticated = !isAnon && session != null, isAnonymous = isAnon)
            }
        }
    }

    fun updateDisplayName(name: String) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "El nombre no puede estar vacío") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, error = null) }
            try {
                auth.updateUser {
                    data { put("display_name", JsonPrimitive(name.trim())) }
                }
                _uiState.update {
                    it.copy(isSavingProfile = false, displayName = name.trim(), profileSavedMessage = "Nombre actualizado")
                }
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: updateDisplayName error — ${e.message}")
                _uiState.update { it.copy(isSavingProfile = false, error = "No se pudo actualizar el nombre: ${e.message}") }
            }
        }
    }

    fun updatePassword(newPassword: String) {
        if (newPassword.length < 6) {
            _uiState.update { it.copy(error = "La contraseña debe tener al menos 6 caracteres") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, error = null) }
            try {
                auth.updateUser { password = newPassword }
                _uiState.update { it.copy(isSavingProfile = false, profileSavedMessage = "Contraseña actualizada") }
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: updatePassword error — ${e.message}")
                _uiState.update { it.copy(isSavingProfile = false, error = "No se pudo actualizar la contraseña: ${e.message}") }
            }
        }
    }

    /**
     * Verifica la contraseña actual re-autenticando al usuario, y si es correcta actualiza a [newPassword].
     * [confirmPassword] debe coincidir con [newPassword].
     */
    fun updatePasswordVerified(currentPassword: String, newPassword: String, confirmPassword: String) {
        val email = _uiState.value.userEmail
        when {
            currentPassword.isBlank() -> {
                _uiState.update { it.copy(error = "Introduce tu contraseña actual") }
                return
            }
            newPassword.length < 6 -> {
                _uiState.update { it.copy(error = "La nueva contraseña debe tener al menos 6 caracteres") }
                return
            }
            newPassword != confirmPassword -> {
                _uiState.update { it.copy(error = "Las contraseñas nuevas no coinciden") }
                return
            }
            email.isBlank() -> {
                _uiState.update { it.copy(error = "No se pudo obtener el correo del usuario") }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, error = null) }
            try {
                // Re-autenticar para verificar contraseña actual
                auth.signInWith(Email) {
                    this.email = email
                    this.password = currentPassword
                }
                // Si la re-autenticación fue bien, actualizar contraseña
                auth.updateUser { password = newPassword }
                _uiState.update { it.copy(isSavingProfile = false, profileSavedMessage = "Contraseña actualizada correctamente") }
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: updatePasswordVerified error — ${e.message}")
                val msg = if (e.message?.contains("Invalid login credentials", ignoreCase = true) == true)
                    "La contraseña actual no es correcta"
                else
                    "No se pudo actualizar la contraseña: ${e.message}"
                _uiState.update { it.copy(isSavingProfile = false, error = msg) }
            }
        }
    }

    fun consumeProfileSavedMessage() = _uiState.update { it.copy(profileSavedMessage = null) }

    fun updateAvatarUrl(url: String) {
        val userId = _uiState.value.userEmail.ifBlank { null } // usamos userId via auth
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, error = null) }
            try {
                val authUserId = auth.currentSessionOrNull()?.user?.id ?: return@launch
                val profile = UserProfile(
                    userId = authUserId,
                    displayName = _uiState.value.displayName,
                    avatarUrl = url.trim().ifBlank { null }
                )
                userProfileRepository?.upsertProfile(profile)
                _uiState.update {
                    it.copy(isSavingProfile = false, avatarUrl = url.trim().ifBlank { null }, profileSavedMessage = "Avatar actualizado")
                }
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: updateAvatarUrl error — ${e.message}")
                _uiState.update { it.copy(isSavingProfile = false, error = "No se pudo actualizar el avatar: ${e.message}") }
            }
        }
    }

    private fun loadAvatarUrl(userId: String?) {
        if (userId == null) return
        viewModelScope.launch {
            try {
                val profile = userProfileRepository?.getProfile(userId)
                if (profile != null) {
                    _uiState.update { it.copy(avatarUrl = profile.avatarUrl, displayName = profile.displayName.ifBlank { it.displayName }) }
                }
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: loadAvatarUrl error — ${e.message}")
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun clearRegistrationPending() = _uiState.update { it.copy(registrationPendingConfirmation = false) }

    /** Sube los bytes como avatar del usuario y actualiza el perfil con la URL resultante. */
    fun uploadAvatarAndSave(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true, error = null) }
            try {
                val userId = auth.currentSessionOrNull()?.user?.id
                    ?: throw Exception("No hay sesión activa")
                val storage = storageService
                    ?: throw Exception("Servicio de almacenamiento no disponible")
                val url = storage.uploadAvatarImage(userId, imageBytes)
                updateAvatarUrl(url)
            } catch (e: Exception) {
                println("DEBUG AuthViewModel: uploadAvatarAndSave error — ${e.message}")
                _uiState.update { it.copy(isSavingProfile = false, error = "No se pudo subir la imagen: ${e.message}") }
            }
        }
    }

    private fun traducirErrorAuth(message: String?): String {
        if (message == null) return "Error desconocido"
        return when {
            message.contains("Invalid login credentials", ignoreCase = true) -> "Correo o contraseña incorrectos"
            message.contains("Email not confirmed", ignoreCase = true) -> "Debes confirmar tu correo antes de iniciar sesión"
            message.contains("User already registered", ignoreCase = true) -> "Ya existe una cuenta con este correo"
            message.contains("Password should be at least", ignoreCase = true) -> "La contraseña debe tener al menos 6 caracteres"
            message.contains("Unable to validate email address", ignoreCase = true) ||
                (message.contains("invalid", ignoreCase = true) && message.contains("email", ignoreCase = true)) ->
                "El correo electrónico no es válido"
            message.contains("rate limit", ignoreCase = true) -> "Demasiados intentos. Espera unos segundos."
            message.contains("New password should be different", ignoreCase = true) ||
                message.contains("same as the old password", ignoreCase = true) ||
                message.contains("password is the same", ignoreCase = true) -> "Elige una contraseña diferente a la anterior"
            else -> message
        }
    }
}