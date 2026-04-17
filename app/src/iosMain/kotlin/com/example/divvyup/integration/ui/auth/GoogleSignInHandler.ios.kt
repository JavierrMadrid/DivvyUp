package com.example.divvyup.integration.ui.auth

// iOS no necesita una implementación especial de GoogleSignInHandler.
// La interfaz GoogleSignInHandler vive en commonMain y en iOS
// el AuthViewModel usa el flujo OAuth estándar (googleSignInHandler = null).
