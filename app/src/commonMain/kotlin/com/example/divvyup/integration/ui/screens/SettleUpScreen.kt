package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreen100
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel

@Composable
internal fun SettleUpScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val transfers = uiState.debtTransfers
    val transferKeys = remember(transfers) {
        transfers.map { "${it.fromParticipantId}-${it.toParticipantId}" }
    }
    val isDark = isSystemInDarkTheme()
    val highlightedContainer = if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest else JungleGreen100
    val highlightedText = if (isDark) MaterialTheme.colorScheme.onSurface else JungleGreenDark

    var selectedKeys by rememberSaveable(transferKeys) {
        mutableStateOf(transferKeys.toSet())
    }

    val selectedTransfers = remember(transfers, selectedKeys) {
        transfers.filter { "${it.fromParticipantId}-${it.toParticipantId}" in selectedKeys }
    }
    val totalSelected = remember(selectedTransfers) { selectedTransfers.sumOf { it.amount } }
    val allSelected = transfers.isNotEmpty() && selectedKeys.size == transfers.size

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = Brush.verticalGradient(colors = listOf(JungleGreen, JungleGreenDark)))
                    .statusBarsPadding()
            ) {
                CenterAlignedTopAppBar(
                    title = { Text("Liquidar cuentas", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Unspecified,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.Unspecified
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                shape = RoundedCornerShape(topStart = DivvyUpTokens.RadiusCard, topEnd = DivvyUpTokens.RadiusCard),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total a liquidar", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${totalSelected.fmt2()} ${uiState.group?.currency ?: "EUR"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = JungleGreenDark
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.createSettlementsForTransfers(selectedTransfers)
                            onBack()
                        },
                        enabled = selectedTransfers.isNotEmpty() && transfers.isNotEmpty() && !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Confirmar liquidación", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (uiState.error != null) {
                item {
                    Snackbar(
                        action = { TextButton(onClick = viewModel::clearError) { Text("Cerrar") } },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) { Text(uiState.error.orEmpty()) }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedKeys.size} de ${transfers.size} seleccionados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            selectedKeys = if (allSelected) emptySet() else transferKeys.toSet()
                        }
                    ) {
                        Text(
                            if (allSelected) "Deseleccionar todos" else "Seleccionar todos",
                            color = JungleGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                HorizontalDivider()
            }

            if (transfers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                        color = highlightedContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅", fontSize = 24.sp)
                            Text(
                                "Las cuentas ya estan saldadas",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = highlightedText
                            )
                        }
                    }
                }
            } else {
                items(transfers, key = { "${it.fromParticipantId}-${it.toParticipantId}" }) { transfer ->
                    val key = "${transfer.fromParticipantId}-${transfer.toParticipantId}"
                    val isSelected = key in selectedKeys
                    TransferCard(
                        transfer = transfer,
                        currency = uiState.group?.currency ?: "EUR",
                        isSelectable = true,
                        isSelected = isSelected,
                        onToggle = {
                            selectedKeys = if (isSelected) selectedKeys - key else selectedKeys + key
                        }
                    )
                }
            }
        }
    }
}


