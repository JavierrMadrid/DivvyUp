package com.example.divvyup.integration.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.integration.ui.components.AppIconButton
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.appOutlinedTextFieldColors
import com.example.divvyup.integration.ui.viewmodel.AddParticipantsViewModel
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.Strings
import com.example.divvyup.integration.ui.components.AppEmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddParticipantsScreen(
    viewModel: AddParticipantsViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navegar cuando el VM indique (LaunchedEffect — side effect, AGENTS.md)
    LaunchedEffect(uiState.navigateToDetail) {
        if (uiState.navigateToDetail) {
            viewModel.consumeNavigation()
            onNavigateToDetail(uiState.groupId)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = Strings.AddParticipants.TITLE,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Strings.AddParticipants.STEP_LABEL,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            BottomBar(
                participantCount = uiState.participants.size,
                onContinue = viewModel::finishAndNavigate
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Formulario de nuevo participante
            AddParticipantForm(
                name = uiState.draft.name,
                email = uiState.draft.email,
                nameError = uiState.draft.nameError,
                isSaving = uiState.isSaving,
                onNameChange = viewModel::updateDraftName,
                onEmailChange = viewModel::updateDraftEmail,
                onAdd = viewModel::addParticipant
            )

            HorizontalDivider()

            // Lista de participantes añadidos
            if (uiState.participants.isEmpty()) {
                ParticipantsEmptyHint(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                Text(
                    text = Strings.AddParticipants.participantsAddedCount(uiState.participants.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.participants, key = { it.id.toString() + it.name }) { participant ->
                        ParticipantChipRow(
                            participant = participant,
                            isSelf = uiState.selfParticipantId == participant.id,
                            onToggleSelf = { viewModel.toggleSelfParticipant(participant.id) },
                            onRemove = { viewModel.removeParticipant(participant) }
                        )
                    }
                }
            }

            // Error Snackbar
            uiState.error?.let { msg ->
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(8_000)
                    viewModel.clearError()
                }
                Snackbar(
                    modifier = Modifier.padding(bottom = 8.dp),
                    action = { TextButton(onClick = viewModel::clearError) { Text(Strings.Common.OK) } }
                ) { Text(msg) }
            }
        }
    }
}

@Composable
private fun AddParticipantForm(
    name: String,
    email: String,
    nameError: Boolean,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emailFocus = remember { FocusRequester() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCardMd),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = Strings.AddParticipants.FORM_HEADING,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(Strings.AddParticipants.FIELD_NAME) },
                placeholder = { Text(Strings.AddParticipants.FIELD_NAME_PLACEHOLDER) },
                isError = nameError,
                supportingText = if (nameError) {
                    { Text(Strings.AddParticipants.ERROR_NAME_REQUIRED) }
                } else null,
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { emailFocus.requestFocus() }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                colors = appOutlinedTextFieldColors()
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text(Strings.AddParticipants.FIELD_EMAIL) },
                placeholder = { Text(Strings.AddParticipants.FIELD_EMAIL_PLACEHOLDER) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailFocus),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                colors = appOutlinedTextFieldColors()
            )

            Button(
                onClick = onAdd,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(Strings.AddParticipants.BUTTON_ADD)
            }
        }
    }
}

@Composable
private fun ParticipantChipRow(
    participant: Participant,
    isSelf: Boolean,
    onToggleSelf: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar con inicial
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.name.first().uppercaseChar().toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = participant.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!participant.email.isNullOrBlank()) {
                        Text(
                            text = participant.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                // Toggle "Soy yo"
                Surface(
                    onClick = onToggleSelf,
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                    color = if (isSelf) JungleGreen else MaterialTheme.colorScheme.surface,
                    border = if (isSelf) null else androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelf) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = Strings.AddParticipants.SELF_BADGE,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelf) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelf) androidx.compose.ui.graphics.Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AppIconButton(
                    onClick = onRemove,
                    icon = Icons.Default.Close,
                    contentDescription = Strings.AddParticipants.A11Y_REMOVE,
                    onClickLabel = Strings.AddParticipants.A11Y_REMOVE_LABEL,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ParticipantsEmptyHint(modifier: Modifier = Modifier) {
    AppEmptyState(
        emoji = Strings.AddParticipants.EMPTY_EMOJI,
        title = Strings.AddParticipants.EMPTY_HEADLINE,
        body = Strings.AddParticipants.EMPTY_SUBTITLE,
        modifier = modifier
    )
}

@Composable
private fun BottomBar(
    participantCount: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
            shadowElevation = DivvyUpTokens.ElevationBottomBar,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusRow),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = if (participantCount == 0) Strings.AddParticipants.CONTINUE_EMPTY
                    else Strings.AddParticipants.continueWithCount(participantCount),
                    fontWeight = FontWeight.SemiBold
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


