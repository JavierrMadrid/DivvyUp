package com.example.divvyup.integration.ui

import androidx.compose.runtime.Composable

@Composable
actual fun SetSystemBarAppearance(useDarkIcons: Boolean) {
    // iOS gestiona el estilo de la status bar vía UIViewController; no-op por ahora.
}

@Composable
actual fun SetNavigationBarAppearance(useDarkIcons: Boolean) {
    // iOS no tiene barra de navegación inferior del sistema en el mismo sentido.
}
