package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.appOutlinedTextFieldColors
import com.example.divvyup.integration.ui.viewmodel.AuthViewModel

@Composable
fun ChangePasswordScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showCurrent by rememberSaveable { mutableStateOf(false) }
    var showNew by rememberSaveable { mutableStateOf(false) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    // Mostrar éxito y volver
    LaunchedEffect(authState.profileSavedMessage) {
        val msg = authState.profileSavedMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        authViewModel.consumeProfileSavedMessage()
        onBack()
    }

    // Mostrar error en snackbar sin bloquear
    LaunchedEffect(authState.error) {
        val err = authState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(err)
        authViewModel.clearError()
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
                    text = "Cambiar contraseña",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Descripción
            Text(
                "Introduce tu contraseña actual para verificar tu identidad y luego escribe la nueva contraseña dos veces para confirmarla.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Formulario
            Surface(
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Contraseña actual
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Contraseña actual") },
                        singleLine = true,
                        visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCurrent = !showCurrent }) {
                                Icon(
                                    if (showCurrent) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        colors = appOutlinedTextFieldColors()
                    )

                    // Nueva contraseña
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nueva contraseña") },
                        singleLine = true,
                        visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showNew = !showNew }) {
                                Icon(
                                    if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        supportingText = {
                            if (newPassword.isNotBlank() && newPassword.length < 6) {
                                Text("Mínimo 6 caracteres", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        colors = appOutlinedTextFieldColors()
                    )

                    // Confirmar nueva contraseña
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar nueva contraseña") },
                        singleLine = true,
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(
                                    if (showConfirm) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        supportingText = {
                            if (confirmPassword.isNotBlank() && confirmPassword != newPassword) {
                                Text("Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        colors = appOutlinedTextFieldColors()
                    )
                }
            }

            val canSubmit = currentPassword.isNotBlank()
                && newPassword.length >= 6
                && newPassword == confirmPassword
                && !authState.isSavingProfile

            Button(
                onClick = {
                    authViewModel.updatePasswordVerified(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword
                    )
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = JungleGreen)
            ) {
                if (authState.isSavingProfile) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Actualizar contraseña", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

