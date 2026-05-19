package com.example.divvyup.integration.ui.theme

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modos de tema disponibles para el usuario.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Singleton que mantiene la preferencia de tema en memoria.
 * La capa de plataforma (MainActivity / MainViewController) puede
 * inicializarlo con el valor almacenado en disco y suscribirse a
 * cambios para persistirlos.
 */
object ThemePreferenceHolder {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
}

/**
 * Preferencia global para notificaciones locales de acciones de gastos.
 * Android la persiste en SharedPreferences y la UI la consume desde Compose.
 */
object NotificationPreferenceHolder {
    private val _spendNotificationsEnabled = MutableStateFlow(true)
    val spendNotificationsEnabled: StateFlow<Boolean> = _spendNotificationsEnabled.asStateFlow()

    fun setSpendNotificationsEnabled(enabled: Boolean) {
        _spendNotificationsEnabled.value = enabled
    }
}

