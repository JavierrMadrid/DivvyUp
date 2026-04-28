package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.integration.ui.rememberImagePickerLauncher
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.theme.appOutlinedTextFieldColors
import com.example.divvyup.integration.ui.viewmodel.AuthViewModel

@Composable
fun UserSettingsScreen(
    authViewModel: AuthViewModel,
    isAuthenticated: Boolean,
    isAnonymous: Boolean,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState.profileSavedMessage) {
        val msg = authState.profileSavedMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        authViewModel.consumeProfileSavedMessage()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Perfil y cuenta",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { padding ->
        if (isAuthenticated) {
            AuthenticatedContent(
                authViewModel = authViewModel,
                onNavigateToChangePassword = onNavigateToChangePassword,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = DivvyUpTokens.ScreenPaddingHLg)
            )
        } else {
            UnauthenticatedContent(
                isAnonymous = isAnonymous,
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToRegister = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = DivvyUpTokens.ScreenPaddingHLg)
            )
        }
    }
}

@Composable
private fun AuthenticatedContent(
    authViewModel: AuthViewModel,
    onNavigateToChangePassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.uiState.collectAsState()

    var isEditMode by rememberSaveable { mutableStateOf(false) }
    var displayName by rememberSaveable(authState.displayName) { mutableStateOf(authState.displayName) }

    // Salir del modo edición si el guardado fue exitoso
    LaunchedEffect(authState.profileSavedMessage) {
        if (authState.profileSavedMessage != null) isEditMode = false
    }

    val logoutButtonHeight = DivvyUpTokens.PrimaryButtonHeight + 24.dp + 16.dp
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    val pickAvatar = rememberImagePickerLauncher { imageBytes ->
        if (imageBytes != null) authViewModel.uploadAvatarAndSave(imageBytes)
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión", fontWeight = FontWeight.Bold) },
            text = { Text("¿Seguro que quieres cerrar sesión?") },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; authViewModel.logout() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill)
                ) { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill)
                ) { Text("Cancelar") }
            }
        )
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = logoutButtonHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapLg)
        ) {
        Spacer(Modifier.height(16.dp))

        // ── Avatar ────────────────────────────────────────────────────────
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(JungleGreen)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (authState.displayName.isNotBlank()) {
                    Text(
                        text = authState.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
            // Badge cámara — abre galería para cambiar avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .shadow(2.dp, CircleShape)
                    .clip(CircleShape)
                    .background(JungleGreenDark)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = pickAvatar, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Cambiar foto de perfil",
                        tint = Color.White,
                        modifier = Modifier.size(DivvyUpTokens.IconSm)
                    )
                }
            }
        }

        Text(
            text = authState.userEmail.ifBlank { "Sesión iniciada" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ── Error ─────────────────────────────────────────────────────────
        authState.error?.let { errorMsg ->
            Surface(
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // ── Sección: Información del perfil ───────────────────────────────
        ProfileSectionCard(
            title = "Información del perfil",
            headerAction = if (!isEditMode) ({
                IconButton(
                    onClick = { isEditMode = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar perfil",
                        tint = JungleGreen,
                        modifier = Modifier.size(DivvyUpTokens.IconSm)
                    )
                }
            }) else null
        ) {
            // Email (solo lectura)
            ProfileFieldReadOnly(
                label = "Correo electrónico",
                value = authState.userEmail.ifBlank { "—" }
            )
            // Nombre para mostrar
            if (isEditMode) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Nombre para mostrar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = appOutlinedTextFieldColors()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapSm)
                ) {
                    OutlinedButton(
                        onClick = {
                            displayName = authState.displayName
                            isEditMode = false
                            authViewModel.clearError()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp, MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(DivvyUpTokens.IconSm))
                        Spacer(Modifier.size(4.dp))
                        Text("Cancelar", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { authViewModel.updateDisplayName(displayName) },
                        enabled = !authState.isSavingProfile && displayName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JungleGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(DivvyUpTokens.IconSm))
                        Spacer(Modifier.size(4.dp))
                        Text("Guardar", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                ProfileFieldReadOnly(
                    label = "Nombre para mostrar",
                    value = authState.displayName.ifBlank { "Sin nombre" }
                )
            }
        }


        // ── Sección: Seguridad ────────────────────────────────────────────
        ProfileSectionCard(title = "Seguridad") {
            Button(
                onClick = onNavigateToChangePassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DivvyUpTokens.PrimaryButtonHeight),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JungleGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(DivvyUpTokens.IconSm))
                Spacer(Modifier.size(DivvyUpTokens.GapSm))
                Text(
                    "Cambiar contraseña",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        } // fin Column scrollable

        // ── Cerrar sesión fijo en la parte inferior ───────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = 24.dp, top = 16.dp)
        ) {
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DivvyUpTokens.PrimaryButtonHeight),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(DivvyUpTokens.IconMd))
                Spacer(Modifier.size(DivvyUpTokens.GapSm))
                Text("Cerrar sesión", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    } // fin Box
}

/**
 * Tarjeta de sección con título en verde prominente y separador bajo el título.
 * Acepta un slot para contenido adicional en la cabecera (p. ej. botón editar).
 */
@Composable
private fun ProfileSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cabecera: título + acción opcional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                headerAction?.invoke()
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Column(verticalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapMd)) {
                content()
            }
        }
    }
}

// Wrapper para pasar headerAction desde los call-sites existentes
@Composable
private fun ProfileSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) = ProfileSectionCard(title = title, modifier = modifier, headerAction = null, content = content)

@Composable
private fun ProfileFieldReadOnly(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun UnauthenticatedContent(
    isAnonymous: Boolean,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Altura total de los dos botones + espaciado + padding inferior
    val bottomBarHeight = DivvyUpTokens.PrimaryButtonHeight * 2 + DivvyUpTokens.GapMd + 24.dp + 16.dp

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomBarHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapLg)
        ) {
            Spacer(Modifier.height(32.dp))

            // Icono ilustrativo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(52.dp)
                )
            }

            Text(
                text = "Sin sesión iniciada",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            // Banner modo invitado
            if (isAnonymous) {
                Surface(
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("☁️", fontSize = 20.sp)
                        Text(
                            "Estás en modo invitado. Tus grupos se guardan en la nube de forma temporal. " +
                            "Crea una cuenta para acceder a ellos desde cualquier dispositivo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Mensaje informativo
            Card(
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Puedes usar DivvyUp sin cuenta, pero si inicias sesión podrás:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    BenefitItem("☁️", "Guardar tus grupos en la nube")
                    BenefitItem("📱", "Acceder desde cualquier dispositivo")
                    BenefitItem("👥", "Unirte a grupos de otros usuarios")
                    BenefitItem("🔒", "Mantener tus datos seguros")
                }
            }
        }

        // ── Botones fijos en la parte inferior ────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = 24.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapMd)
        ) {
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DivvyUpTokens.PrimaryButtonHeight),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JungleGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(DivvyUpTokens.IconMd))
                Spacer(Modifier.size(10.dp))
                Text(
                    "Iniciar sesión",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DivvyUpTokens.PrimaryButtonHeight),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, MaterialTheme.colorScheme.primary
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Crear cuenta nueva",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun BenefitItem(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Normal
        )
    }
}
