package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.viewmodel.JoinGroupParticipantViewModel

@Composable
fun JoinGroupParticipantScreen(
    viewModel: JoinGroupParticipantViewModel,
    onBack: () -> Unit,
    onJoinedGroup: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.navigateToGroupDetail) {
        if (uiState.navigateToGroupDetail) {
            viewModel.consumeNavigateToGroupDetail()
            onJoinedGroup(uiState.groupId)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text(
                    text = "Unirse al grupo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        bottomBar = {
            Button(
                onClick = viewModel::confirmSelection,
                enabled = !uiState.isLoading && !uiState.isSaving && uiState.selectedParticipantId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DivvyUpTokens.ScreenPaddingHLg, vertical = 12.dp)
                    .height(DivvyUpTokens.PrimaryButtonHeight),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(DivvyUpTokens.IconSm), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Continuar", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = DivvyUpTokens.ScreenPaddingHLg, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapMd)
        ) {
            item {
                Text(
                    text = "Te han invitado a \"${uiState.groupName}\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Selecciona qué participante eres para vincular tu usuario a este grupo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(uiState.participants, key = { it.id }) { participant ->
                val isSelected = uiState.selectedParticipantId == participant.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { viewModel.selectParticipant(participant.id) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, CircleShape)
                            .size(DivvyUpTokens.IconLg + 12.dp)
                            .clip(CircleShape)
                            .background(JungleGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = participant.name.first().uppercaseChar().toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.size(DivvyUpTokens.GapMd))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(participant.name, fontWeight = FontWeight.SemiBold)
                        participant.email?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = JungleGreen)
                    }
                }
            }
        }

        uiState.error?.let { message ->
            LaunchedEffect(message) { viewModel.clearError() }
            Snackbar(
                modifier = Modifier
                    .padding(DivvyUpTokens.ScreenPaddingHLg)
                    .fillMaxWidth(),
                action = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
            ) {
                Text(message)
            }
        }
    }
}
