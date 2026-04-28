package com.example.divvyup.integration.ui.screens.groupsettings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.rememberImagePickerLauncher
import com.example.divvyup.integration.ui.screens.isSettlementCategory
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel

@Composable
fun GroupSettingsScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    onNavigateToAddParticipant: () -> Unit,
    onShareInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val group = uiState.group
    val isOwner = uiState.isOwner
    val snackbarHostState = remember { SnackbarHostState() }


    LaunchedEffect(uiState.settingsSavedMessage) {
        val message = uiState.settingsSavedMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeSettingsSavedMessage()
    }

    // Estado local de edición del grupo (no se aplican hasta "Guardar cambios")
    var groupName by rememberSaveable(group?.name) { mutableStateOf(group?.name ?: "") }
    var groupDescription by rememberSaveable(group?.description) {
        mutableStateOf(
            group?.description ?: ""
        )
    }
    var groupCurrency by rememberSaveable(group?.currency) {
        mutableStateOf(
            group?.currency ?: "EUR"
        )
    }
    var groupDefaultCategoryId by rememberSaveable(group?.defaultCategoryId) { mutableStateOf(group?.defaultCategoryId) }
    var nameError by rememberSaveable { mutableStateOf(false) }

    // Estado local para reparto por defecto pendiente de guardar
    var pendingSplitPercentages by remember(uiState.defaultSplitPercentages) {
        mutableStateOf(uiState.defaultSplitPercentages)
    }

    // Dialogs
    var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDefaultSplitDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteCategoryConfirm by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDeleteParticipantConfirm by rememberSaveable { mutableStateOf<Long?>(null) }

    val pickGroupPhoto = rememberImagePickerLauncher { imageBytes ->
        if (imageBytes != null) viewModel.uploadGroupPhotoAndSave(imageBytes)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            OwnerSaveBar(
                isVisible = isOwner,
                isLoading = uiState.isLoading,
                onSave = {
                    if (groupName.isBlank()) {
                        nameError = true
                        return@OwnerSaveBar
                    }
                    viewModel.saveGroupSettings(
                        name = groupName,
                        description = groupDescription,
                        currency = groupCurrency,
                        defaultSplitPercentages = pendingSplitPercentages,
                        defaultCategoryId = groupDefaultCategoryId
                    )
                }
            )
        },
        topBar = {
            GroupSettingsTopBar(onBack = onBack)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            GroupSettingsContent(
                uiState = uiState,
                padding = padding,
                groupName = groupName,
                onGroupNameChange = {
                    groupName = it
                    nameError = false
                },
                groupDescription = groupDescription,
                onGroupDescriptionChange = { groupDescription = it },
                groupCurrency = groupCurrency,
                onGroupCurrencyChange = { groupCurrency = it },
                groupDefaultCategoryId = groupDefaultCategoryId,
                onGroupDefaultCategoryIdChange = { groupDefaultCategoryId = it },
                nameError = nameError,
                pendingSplitPercentages = pendingSplitPercentages,
                onPickGroupPhoto = pickGroupPhoto,
                onShareInvite = onShareInvite,
                onNavigateToAddParticipant = onNavigateToAddParticipant,
                onSelectMyParticipant = viewModel::selectMyParticipant,
                onDeleteParticipantRequest = { showDeleteParticipantConfirm = it },
                onShowDefaultSplitDialog = { showDefaultSplitDialog = true },
                onShowAddCategoryDialog = { showAddCategoryDialog = true },
                onDeleteCategoryRequest = { showDeleteCategoryConfirm = it },
                onUpdateCategoryBudget = viewModel::updateCategoryBudget
            )

            SettingsErrorSnackbar(
                errorMessage = uiState.error,
                onClearError = viewModel::clearError,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // ── Diálogo: Añadir categoría ──────────────────────────────────────────
    if (showAddCategoryDialog) {
        val existingCustomCategories = uiState.categories.filter {
            it.groupId != null && !it.isDefault && !it.isSettlementCategory()
        }
        AddCategoryDialog(
            existingCategories = existingCustomCategories,
            onConfirm = { name, icon ->
                viewModel.createCategory(name, icon)
                showAddCategoryDialog = false
            },
            onDismiss = { showAddCategoryDialog = false }
        )
    }

    // ── Diálogo: Confirmar eliminar categoría ──────────────────────────────
    showDeleteCategoryConfirm?.let { catId ->
        val cat = uiState.categories.firstOrNull { it.id == catId }
        ConfirmDeleteCategoryDialog(
            categoryName = cat?.name,
            onConfirm = { viewModel.deleteCategory(catId); showDeleteCategoryConfirm = null },
            onDismiss = { showDeleteCategoryConfirm = null }
        )
    }

    // ── Diálogo: Confirmar eliminar participante ───────────────────────────
    showDeleteParticipantConfirm?.let { participantId ->
        val participant = uiState.participants.firstOrNull { it.id == participantId }
        ConfirmDeleteParticipantDialog(
            participantName = participant?.name,
            onConfirm = { viewModel.deleteParticipant(participantId); showDeleteParticipantConfirm = null },
            onDismiss = { showDeleteParticipantConfirm = null }
        )
    }

    // ── Diálogo: Reparto por defecto ───────────────────────────────────────
    if (showDefaultSplitDialog) {
        DefaultSplitDialog(
            participants = uiState.participants,
            currentPercentages = pendingSplitPercentages,
            onConfirm = { percentages ->
                pendingSplitPercentages = percentages
                showDefaultSplitDialog = false
            },
            onDismiss = { showDefaultSplitDialog = false }
        )
    }
}


@Composable
private fun SettingsErrorSnackbar(
    errorMessage: String?,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    errorMessage?.let { message ->
        Snackbar(
            modifier = modifier.padding(20.dp),
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            action = { TextButton(onClick = onClearError) { Text("OK") } }
        ) {
            Text(message)
        }
    }
}