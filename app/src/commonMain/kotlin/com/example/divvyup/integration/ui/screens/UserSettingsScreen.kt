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
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.material3.Switch
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
import com.example.divvyup.integration.ui.Strings
import com.example.divvyup.integration.ui.ThemedSystemBarAppearance
import com.example.divvyup.integration.ui.rememberImagePickerLauncher
import com.example.divvyup.integration.ui.components.AppIconButton
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.theme.NotificationPreferenceHolder
import com.example.divvyup.integration.ui.theme.ThemeMode
import com.example.divvyup.integration.ui.theme.ThemePreferenceHolder
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
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(authState.profileSavedMessage) {
        val msg = authState.profileSavedMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        authViewModel.consumeProfileSavedMessage()
    }

    ThemedSystemBarAppearance()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        start = DivvyUpTokens.ScreenPaddingHLg,
                        end = DivvyUpTokens.ScreenPaddingHLg,
                        top = 12.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Strings.UserSettings.TITLE,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
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
            title = { Text(Strings.Common.LOGOUT, fontWeight = FontWeight.Bold) },
            text = { Text(Strings.UserSettings.LOGOUT_CONFIRM_BODY) },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; authViewModel.logout() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill)
                ) { Text(Strings.Common.LOGOUT, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill)
                ) { Text(Strings.Common.CANCEL) }
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
                    .shadow(DivvyUpTokens.ElevationCard, CircleShape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
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
                    .shadow(DivvyUpTokens.ElevationCard, CircleShape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f))
                    .clip(CircleShape)
                    .background(JungleGreenDark)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AppIconButton(
                    onClick = pickAvatar,
                    icon = Icons.Default.CameraAlt,
                    contentDescription = Strings.UserSettings.A11Y_CHANGE_AVATAR,
                    onClickLabel = Strings.UserSettings.A11Y_CHANGE_AVATAR,
                    tint = Color.White
                )
            }
        }

        Text(
            text = authState.userEmail.ifBlank { Strings.UserSettings.EMAIL_FALLBACK },
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
            title = Strings.UserSettings.SECTION_PROFILE,
            headerAction = if (!isEditMode) ({
                IconButton(
                    onClick = { isEditMode = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = Strings.UserSettings.A11Y_EDIT_PROFILE,
                        tint = JungleGreen,
                        modifier = Modifier.size(DivvyUpTokens.IconSm)
                    )
                }
            }) else null
        ) {
            // Email (solo lectura)
            ProfileFieldReadOnly(
                label = Strings.UserSettings.FIELD_EMAIL,
                value = authState.userEmail.ifBlank { Strings.UserSettings.EMAIL_EMDASH_FALLBACK }
            )
            // Nombre para mostrar
            if (isEditMode) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(Strings.UserSettings.FIELD_DISPLAY_NAME) },
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
                        Text(Strings.Common.CANCEL, fontWeight = FontWeight.SemiBold)
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
                        Text(Strings.Common.SAVE, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                ProfileFieldReadOnly(
                    label = Strings.UserSettings.FIELD_DISPLAY_NAME,
                    value = authState.displayName.ifBlank { Strings.UserSettings.DISPLAY_NAME_FALLBACK }
                )
            }
        }


        // ── Sección: Seguridad ────────────────────────────────────────────
        ProfileSectionCard(title = Strings.UserSettings.SECTION_SECURITY) {
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
                    Strings.UserSettings.CHANGE_PASSWORD,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ── Sección: Apariencia ───────────────────────────────────────────
        ThemeSection()
        NotificationSection()

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
                Text(Strings.Common.LOGOUT, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = DivvyUpTokens.ElevationCard,
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
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
                    .shadow(DivvyUpTokens.ElevationCard, CircleShape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
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
                text = Strings.UserSettings.UNAUTH_HEADLINE,
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
                        Text(Strings.UserSettings.UNAUTH_EMOJI, fontSize = 20.sp)
                        Text(
                            Strings.UserSettings.UNAUTH_GUEST_TEXT,
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
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = DivvyUpTokens.ElevationCard,
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        Strings.UserSettings.UNAUTH_PERKS_HEADER,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    BenefitItem(Strings.UserSettings.UNAUTH_PERK_CLOUD_EMOJI, Strings.UserSettings.UNAUTH_PERK_CLOUD)
                    BenefitItem(Strings.UserSettings.UNAUTH_PERK_DEVICE_EMOJI, Strings.UserSettings.UNAUTH_PERK_DEVICE)
                    BenefitItem(Strings.UserSettings.UNAUTH_PERK_COMMUNITY_EMOJI, Strings.UserSettings.UNAUTH_PERK_COMMUNITY)
                    BenefitItem(Strings.UserSettings.UNAUTH_PERK_SECURE_EMOJI, Strings.UserSettings.UNAUTH_PERK_SECURE)
                }
            }

            // ── Sección: Apariencia ───────────────────────────────────────
            ThemeSection()
            NotificationSection()
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
                    Strings.UserSettings.BUTTON_LOGIN,
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
                    Strings.UserSettings.BUTTON_REGISTER,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun NotificationSection(modifier: Modifier = Modifier) {
    val enabled by NotificationPreferenceHolder.spendNotificationsEnabled.collectAsState()

    ProfileSectionCard(title = Strings.UserSettings.SECTION_NOTIFICATIONS, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapSm)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(DivvyUpTokens.IconSm)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = Strings.UserSettings.NOTIFICATION_FIELD,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = Strings.UserSettings.NOTIFICATION_HELPER,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { NotificationPreferenceHolder.setSpendNotificationsEnabled(it) }
            )
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

// ---------------------------------------------------------------------------
// ThemeSection — selector de tema (sistema / claro / oscuro)
// ---------------------------------------------------------------------------

@Composable
internal fun ThemeSection(modifier: Modifier = Modifier) {
    val currentMode by ThemePreferenceHolder.themeMode.collectAsState()

    ProfileSectionCard(title = Strings.UserSettings.SECTION_APPEARANCE, modifier = modifier) {
        Text(
            text = Strings.UserSettings.THEME_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(DivvyUpTokens.GapSm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapSm)
        ) {
            ThemeModeChip(
                label = Strings.UserSettings.THEME_SYSTEM,
                icon = Icons.Default.Brightness6,
                selected = currentMode == ThemeMode.SYSTEM,
                onClick = { ThemePreferenceHolder.setThemeMode(ThemeMode.SYSTEM) },
                modifier = Modifier.weight(1f)
            )
            ThemeModeChip(
                label = Strings.UserSettings.THEME_LIGHT,
                icon = Icons.Default.Brightness7,
                selected = currentMode == ThemeMode.LIGHT,
                onClick = { ThemePreferenceHolder.setThemeMode(ThemeMode.LIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeModeChip(
                label = Strings.UserSettings.THEME_DARK,
                icon = Icons.Default.Brightness4,
                selected = currentMode == ThemeMode.DARK,
                onClick = { ThemePreferenceHolder.setThemeMode(ThemeMode.DARK) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ThemeModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant
    val contentColor   = if (selected) Color.White  else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
        color = containerColor,
        modifier = modifier.height(DivvyUpTokens.ControlHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(DivvyUpTokens.IconSm)
            )
            Spacer(Modifier.size(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

