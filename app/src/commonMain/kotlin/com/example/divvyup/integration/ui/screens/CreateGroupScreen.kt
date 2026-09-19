package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.theme.appOutlinedTextFieldColors
import com.example.divvyup.integration.ui.viewmodel.GroupListViewModel
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.Strings
import com.example.divvyup.integration.ui.ThemedSystemBarAppearance

/**
 * Pantalla "Crear grupo" — estilo fintech, sin dialog.
 * Se invoca desde GroupListScreen en lugar de abrir un AlertDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    viewModel: GroupListViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var name        by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var currency    by rememberSaveable { mutableStateOf("EUR") }
    var nameError   by rememberSaveable { mutableStateOf(false) }

    ThemedSystemBarAppearance()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Top bar flotante estilo fintech — sin fondo
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
                        contentDescription = Strings.CreateGroup.A11Y_BACK,
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = Strings.CreateGroup.TITLE,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                // Placeholder para centrar título
                Spacer(Modifier.size(48.dp))
            }
        },
        bottomBar = {
            Surface(
                shadowElevation = DivvyUpTokens.ElevationBottomBar,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                return@Button
                            }
                            viewModel.createGroup(name.trim(), description.trim(), currency)
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DivvyUpTokens.PrimaryButtonHeight)
                            .shadow(
                                elevation = DivvyUpTokens.ElevationRaised,
                                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                ambientColor = JungleGreen.copy(alpha = 0.2f),
                                spotColor = JungleGreen.copy(alpha = 0.35f)
                            ),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JungleGreen,
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                Strings.CreateGroup.NEXT_BUTTON,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header decorativo con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        brush = Brush.horizontalGradient(listOf(JungleGreen, JungleGreenDark)),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusHero)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        Strings.CreateGroup.HERO_HEADLINE,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        Strings.CreateGroup.HERO_SUBTITLE,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Nombre del grupo
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    Strings.CreateGroup.FIELD_NAME,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    placeholder = { Text(Strings.CreateGroup.FIELD_NAME_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text(Strings.CreateGroup.ERROR_NAME_REQUIRED) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusCardMd),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    colors = appOutlinedTextFieldColors()
                )
            }

            // Descripción
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    Strings.CreateGroup.FIELD_DESCRIPTION,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(Strings.CreateGroup.FIELD_DESCRIPTION_PLACEHOLDER, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusCardMd),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    colors = appOutlinedTextFieldColors()
                )
            }

            // Divisa
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    Strings.CreateGroup.FIELD_CURRENCY,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                CreateGroupCurrencySelector(
                    selected = currency,
                    onSelect = { currency = it }
                )
            }

            // Error snackbar
            uiState.error?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(8_000)
                    viewModel.clearError()
                }
                Snackbar(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = { TextButton(onClick = viewModel::clearError) { Text(Strings.Common.OK) } }
                ) { Text(msg) }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGroupCurrencySelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currencies = listOf(
        "EUR" to Strings.CreateGroup.CURRENCY_EUR,
        "USD" to Strings.CreateGroup.CURRENCY_USD,
        "GBP" to Strings.CreateGroup.CURRENCY_GBP,
        "MXN" to Strings.CreateGroup.CURRENCY_MXN,
        "ARS" to Strings.CreateGroup.CURRENCY_ARS,
        "COP" to Strings.CreateGroup.CURRENCY_COP
    )

    // Pills de selección de divisa
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(currencies.size) { idx ->
            val (code, label) = currencies[idx]
            val isSelected = selected == code
            Surface(
                onClick = { onSelect(code) },
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                color = if (isSelected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(40.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


