package com.example.divvyup.integration.ui.screens.groupsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.integration.ui.screens.fmt2
import com.example.divvyup.integration.ui.screens.isSettlementCategory
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.theme.appOutlinedTextFieldColors
import com.example.divvyup.integration.ui.viewmodel.GroupDetailUiState

@Composable
internal fun GroupSettingsTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(JungleGreen, JungleGreenDark)
                )
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Text(
                text = "Ajustes del grupo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun OwnerSaveBar(
    isVisible: Boolean,
    isLoading: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = onSave,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
            colors = ButtonDefaults.buttonColors(
                containerColor = JungleGreen,
                contentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(DivvyUpTokens.IconSm)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text("Guardar cambios", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun GroupSettingsContent(
    uiState: GroupDetailUiState,
    padding: PaddingValues,
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    groupDescription: String,
    onGroupDescriptionChange: (String) -> Unit,
    groupCurrency: String,
    onGroupCurrencyChange: (String) -> Unit,
    groupDefaultCategoryId: Long?,
    onGroupDefaultCategoryIdChange: (Long?) -> Unit,
    nameError: Boolean,
    pendingSplitPercentages: Map<Long, Double>,
    onPickGroupPhoto: () -> Unit,
    onShareInvite: () -> Unit,
    onNavigateToAddParticipant: () -> Unit,
    onSelectMyParticipant: (Long) -> Unit,
    onDeleteParticipantRequest: (Long) -> Unit,
    onShowDefaultSplitDialog: () -> Unit,
    onShowAddCategoryDialog: () -> Unit,
    onDeleteCategoryRequest: (Long) -> Unit,
    onUpdateCategoryBudget: (Long, Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isOwner) {
        OwnerGroupSettingsContent(
            uiState = uiState,
            padding = padding,
            groupName = groupName,
            onGroupNameChange = onGroupNameChange,
            groupDescription = groupDescription,
            onGroupDescriptionChange = onGroupDescriptionChange,
            groupCurrency = groupCurrency,
            onGroupCurrencyChange = onGroupCurrencyChange,
            groupDefaultCategoryId = groupDefaultCategoryId,
            onGroupDefaultCategoryIdChange = onGroupDefaultCategoryIdChange,
            nameError = nameError,
            pendingSplitPercentages = pendingSplitPercentages,
            onPickGroupPhoto = onPickGroupPhoto,
            onShareInvite = onShareInvite,
            onNavigateToAddParticipant = onNavigateToAddParticipant,
            onSelectMyParticipant = onSelectMyParticipant,
            onDeleteParticipantRequest = onDeleteParticipantRequest,
            onShowDefaultSplitDialog = onShowDefaultSplitDialog,
            onShowAddCategoryDialog = onShowAddCategoryDialog,
            onDeleteCategoryRequest = onDeleteCategoryRequest,
            onUpdateCategoryBudget = onUpdateCategoryBudget,
            modifier = modifier
        )
    } else {
        MemberGroupSettingsContent(
            uiState = uiState,
            padding = padding,
            pendingSplitPercentages = pendingSplitPercentages,
            onSelectMyParticipant = onSelectMyParticipant,
            modifier = modifier
        )
    }
}

@Composable
private fun OwnerGroupSettingsContent(
    uiState: GroupDetailUiState,
    padding: PaddingValues,
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    groupDescription: String,
    onGroupDescriptionChange: (String) -> Unit,
    groupCurrency: String,
    onGroupCurrencyChange: (String) -> Unit,
    groupDefaultCategoryId: Long?,
    onGroupDefaultCategoryIdChange: (Long?) -> Unit,
    nameError: Boolean,
    pendingSplitPercentages: Map<Long, Double>,
    onPickGroupPhoto: () -> Unit,
    onShareInvite: () -> Unit,
    onNavigateToAddParticipant: () -> Unit,
    onSelectMyParticipant: (Long) -> Unit,
    onDeleteParticipantRequest: (Long) -> Unit,
    onShowDefaultSplitDialog: () -> Unit,
    onShowAddCategoryDialog: () -> Unit,
    onDeleteCategoryRequest: (Long) -> Unit,
    onUpdateCategoryBudget: (Long, Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            OwnerGroupInfoSection(
                groupName = groupName,
                onGroupNameChange = onGroupNameChange,
                groupDescription = groupDescription,
                onGroupDescriptionChange = onGroupDescriptionChange,
                groupCurrency = groupCurrency,
                onGroupCurrencyChange = onGroupCurrencyChange,
                groupDefaultCategoryId = groupDefaultCategoryId,
                onGroupDefaultCategoryIdChange = onGroupDefaultCategoryIdChange,
                categories = uiState.categories,
                nameError = nameError,
                onPickGroupPhoto = onPickGroupPhoto,
                onShareInvite = onShareInvite
            )
        }

        item {
            ParticipantsSection(
                participants = uiState.participants,
                pendingSplitPercentages = pendingSplitPercentages,
                myParticipantId = uiState.myParticipantId,
                isOwner = true,
                onSelectMyParticipant = onSelectMyParticipant,
                onDeleteParticipantRequest = onDeleteParticipantRequest,
                onNavigateToAddParticipant = onNavigateToAddParticipant,
                onShowDefaultSplitDialog = onShowDefaultSplitDialog
            )
        }

        item {
            OwnerCustomCategoriesSection(
                categories = uiState.categories,
                currency = uiState.group?.currency ?: "EUR",
                onShowAddCategoryDialog = onShowAddCategoryDialog,
                onDeleteCategoryRequest = onDeleteCategoryRequest,
                onUpdateCategoryBudget = onUpdateCategoryBudget
            )
        }
    }
}

@Composable
private fun MemberGroupSettingsContent(
    uiState: GroupDetailUiState,
    padding: PaddingValues,
    pendingSplitPercentages: Map<Long, Double>,
    onSelectMyParticipant: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 20.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { MemberReadonlyBanner() }

        item {
            ParticipantsSection(
                participants = uiState.participants,
                pendingSplitPercentages = pendingSplitPercentages,
                myParticipantId = uiState.myParticipantId,
                isOwner = false,
                onSelectMyParticipant = onSelectMyParticipant,
                onDeleteParticipantRequest = {},
                onNavigateToAddParticipant = {},
                onShowDefaultSplitDialog = {}
            )
        }
    }
}

@Composable
private fun MemberReadonlyBanner(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapMd)
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(DivvyUpTokens.IconMd)
            )
            Text(
                "Solo el creador del grupo puede editar su información y categorías. Aquí puedes indicar cuál eres tú.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun OwnerGroupInfoSection(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    groupDescription: String,
    onGroupDescriptionChange: (String) -> Unit,
    groupCurrency: String,
    onGroupCurrencyChange: (String) -> Unit,
    groupDefaultCategoryId: Long?,
    onGroupDefaultCategoryIdChange: (Long?) -> Unit,
    categories: List<Category>,
    nameError: Boolean,
    onPickGroupPhoto: () -> Unit,
    onShareInvite: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(
        title = "Información del grupo",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(JungleGreen)
                            .border(
                                3.dp,
                                MaterialTheme.colorScheme.background,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = groupName.take(1).uppercase().ifBlank { "G" },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .shadow(2.dp, CircleShape)
                            .clip(CircleShape)
                            .background(JungleGreenDark)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.background,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onPickGroupPhoto,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Cambiar foto del grupo",
                                tint = Color.White,
                                modifier = Modifier.size(DivvyUpTokens.IconSm)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = groupName,
                onValueChange = onGroupNameChange,
                label = { Text("Nombre del grupo *") },
                isError = nameError,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = appOutlinedTextFieldColors()
            )
            if (nameError) {
                Text(
                    "El nombre no puede estar vacío",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            OutlinedTextField(
                value = groupDescription,
                onValueChange = onGroupDescriptionChange,
                label = { Text("Descripción (opcional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = appOutlinedTextFieldColors()
            )
            Text(
                "Moneda",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CURRENCIES) { currency ->
                    val isSelected = groupCurrency == currency
                    Surface(
                        onClick = { onGroupCurrencyChange(currency) },
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                        color = if (isSelected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currency,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            val visibleCategories = categories.filterNot {
                it.name.equals("Liquidación", ignoreCase = true)
            }
            if (visibleCategories.isNotEmpty()) {
                Text(
                    "Categoría por defecto",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        val isSelected = groupDefaultCategoryId == null
                        Surface(
                            onClick = { onGroupDefaultCategoryIdChange(null) },
                            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                            color = if (isSelected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("📦", fontSize = 14.sp)
                                Text(
                                    "Ninguna",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    items(visibleCategories) { category ->
                        val isSelected = groupDefaultCategoryId == category.id
                        Surface(
                            onClick = { onGroupDefaultCategoryIdChange(category.id) },
                            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                            color = if (isSelected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(category.icon, fontSize = 14.sp)
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onShareInvite,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JungleGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Compartir enlace de invitación",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ParticipantsSection(
    participants: List<Participant>,
    pendingSplitPercentages: Map<Long, Double>,
    myParticipantId: Long?,
    isOwner: Boolean,
    onSelectMyParticipant: (Long) -> Unit,
    onDeleteParticipantRequest: (Long) -> Unit,
    onNavigateToAddParticipant: () -> Unit,
    onShowDefaultSplitDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSectionCard(
        title = "Participantes",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (participants.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Reparto",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.width(DivvyUpTokens.GapSm))
                    Text(
                        "Soy yo",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                    if (isOwner) Spacer(Modifier.width(36.dp))
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            participants.forEachIndexed { index, participant ->
                val avatarColor =
                    settingsAvatarPalette[participant.name.length % settingsAvatarPalette.size]
                val pct = pendingSplitPercentages[participant.id]
                val isMe = myParticipantId == participant.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isMe) JungleGreen else avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            participant.name.first().uppercaseChar().toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                participant.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isMe) {
                                Surface(
                                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                    color = JungleGreen
                                ) {
                                    Text(
                                        "Yo",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 2.dp
                                        )
                                    )
                                }
                            }
                        }
                        participant.email?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.width(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (pct != null) "${pct.fmt2s()}%" else "—",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pct != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(Modifier.width(DivvyUpTokens.GapSm))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isMe) JungleGreen else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { onSelectMyParticipant(participant.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Soy yo",
                                tint = if (isMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (isOwner) {
                        IconButton(
                            onClick = { onDeleteParticipantRequest(participant.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar participante",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(DivvyUpTokens.IconSm)
                            )
                        }
                    }
                }
                if (index < participants.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            if (participants.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
            }

            if (isOwner) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onNavigateToAddParticipant,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JungleGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(DivvyUpTokens.IconSm)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Añadir",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Button(
                        onClick = onShowDefaultSplitDialog,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JungleGreen,
                            contentColor = Color.White
                        ),
                        enabled = participants.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Percent,
                            contentDescription = null,
                            modifier = Modifier.size(DivvyUpTokens.IconSm)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Reparto",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerCustomCategoriesSection(
    categories: List<Category>,
    currency: String,
    onShowAddCategoryDialog: () -> Unit,
    onDeleteCategoryRequest: (Long) -> Unit,
    onUpdateCategoryBudget: (Long, Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    var editBudgetCat by remember { mutableStateOf<Category?>(null) }
    val customCategories = categories.filter {
        it.groupId != null && !it.isDefault && !it.isSettlementCategory()
    }

    SettingsSectionCard(
        title = "Categorías personalizadas",
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            if (customCategories.isEmpty()) {
                Text(
                    "Aún no has creado categorías para este grupo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                customCategories.forEachIndexed { index, category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(category.icon, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (category.budget != null) {
                                Text(
                                    "Presupuesto: ${category.budget.fmt2()} $currency/mes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(
                            onClick = { editBudgetCat = category },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Wallet,
                                contentDescription = "Presupuesto",
                                tint = if (category.budget != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(DivvyUpTokens.IconSm)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteCategoryRequest(category.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar categoría",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(DivvyUpTokens.IconSm)
                            )
                        }
                    }
                    if (index < customCategories.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = onShowAddCategoryDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DivvyUpTokens.PrimaryButtonHeight),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JungleGreen,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(DivvyUpTokens.IconSm)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Nueva categoría",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    editBudgetCat?.let { category ->
        BudgetEditDialog(
            category = category,
            currency = currency,
            onConfirm = { budget ->
                onUpdateCategoryBudget(category.id, budget)
                editBudgetCat = null
            },
            onDismiss = { editBudgetCat = null }
        )
    }
}

