package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.integration.ui.theme.*
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import com.example.divvyup.integration.ui.theme.DivvyUpTokens

// Emojis predefinidos para elegir icono de categoría
private val EMOJI_PICKER = listOf(
    "🍔","🍕","🍣","🍜","☕","🛒","🚗","✈️","🏨","🎬",
    "🎮","🎁","💊","🏥","⚽","🏋️","📚","👗","💇","🔧",
    "💡","📱","🖥️","🎵","🍺","🍷","🌴","🏖️","🎂","💰",
    "🏠","🚿","🧺","🐾","🌿","⛽","🅿️","🎨","🧳","📦"
)

private val CURRENCIES = listOf("EUR", "USD", "GBP", "JPY", "MXN", "ARS", "CLP", "COP", "BRL", "CHF")

// Paleta de avatares (misma que en GroupDetailScreen para consistencia)
private val settingsAvatarPalette = listOf(
    JungleGreen, JungleGreenDark, BarkBrown,
    MossGold, Soil, JungleGreenMid,
    BarkBrownDark, Amber
)

private fun Double.fmt2s(): String {
    val rounded = kotlin.math.round(this * 100) / 100.0
    val intPart = rounded.toLong()
    val decPart = kotlin.math.abs((rounded - intPart) * 100).toLong()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}

@Composable
fun GroupSettingsScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    onNavigateToAddParticipant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val group = uiState.group

    // Estado local de edición del grupo (no se aplican hasta "Guardar cambios")
    var groupName        by rememberSaveable(group?.name)        { mutableStateOf(group?.name ?: "") }
    var groupDescription by rememberSaveable(group?.description) { mutableStateOf(group?.description ?: "") }
    var groupCurrency    by rememberSaveable(group?.currency)    { mutableStateOf(group?.currency ?: "EUR") }
    var nameError        by rememberSaveable { mutableStateOf(false) }

    // Estado local para reparto por defecto pendiente de guardar
    var pendingSplitPercentages by remember(uiState.defaultSplitPercentages) {
        mutableStateOf(uiState.defaultSplitPercentages)
    }

    // Dialogs
    var showAddCategoryDialog     by rememberSaveable { mutableStateOf(false) }
    var showDefaultSplitDialog    by rememberSaveable { mutableStateOf(false) }
    var showDeleteCategoryConfirm by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDeleteParticipantConfirm by rememberSaveable { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        if (groupName.isBlank()) { nameError = true; return@Button }
                        viewModel.updateGroup(groupName, groupDescription, groupCurrency)
                        viewModel.setDefaultSplitPercentages(pendingSplitPercentages)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                    colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar cambios", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
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
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Sección: Información del grupo ─────────────────────────────
            item {
                SettingsSectionCard(title = "Información del grupo") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it; nameError = false },
                            label = { Text("Nombre del grupo *") },
                            isError = nameError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
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
                            onValueChange = { groupDescription = it },
                            label = { Text("Descripción (opcional)") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                            colors = appOutlinedTextFieldColors()
                        )
                        // Selector de moneda
                        Text(
                            "Moneda",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(CURRENCIES) { currency ->
                                val isSelected = groupCurrency == currency
                                Surface(
                                    onClick = { groupCurrency = currency },
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
                                            color = if (isSelected) Color.White
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Sección: Participantes ─────────────────────────────────────
            item {
                SettingsSectionCard(title = "Participantes") {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        uiState.participants.forEachIndexed { index, participant ->
                            val avatarColor = settingsAvatarPalette[participant.name.length % settingsAvatarPalette.size]
                            val pct = pendingSplitPercentages[participant.id]
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
                                        .background(avatarColor),
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
                                    Text(
                                        participant.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    participant.email?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                // Badge con el % configurado
                                if (pct != null) {
                                    Surface(
                                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                        color = JungleGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "${pct.fmt2s()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                }
                                IconButton(
                                    onClick = { showDeleteParticipantConfirm = participant.id },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar participante",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (index < uiState.participants.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }

                        if (uiState.participants.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Botón añadir participante
                            Button(
                                onClick = onNavigateToAddParticipant,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                                colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Añadir", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                            // Botón ajustar reparto por defecto
                            Button(
                                onClick = { showDefaultSplitDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                                colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White),
                                enabled = uiState.participants.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reparto", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── Sección: Categorías personalizadas ─────────────────────────
            item {
                SettingsSectionCard(title = "Categorías personalizadas") {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        val customCategories = uiState.categories.filter {
                            it.groupId != null && !it.isDefault && !it.isSettlementCategory()
                        }
                        if (customCategories.isEmpty()) {
                            Text(
                                "Aún no has creado categorías para este grupo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            customCategories.forEachIndexed { index, cat ->
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
                                        Text(cat.icon, fontSize = 20.sp)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        cat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { showDeleteCategoryConfirm = cat.id },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Eliminar categoría",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
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
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                            colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nueva categoría", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── Snackbar de error ──────────────────────────────────────────────
        uiState.error?.let { errorMsg ->
            Box(modifier = Modifier.fillMaxSize()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(20.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
                ) { Text(errorMsg) }
            }
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
        AlertDialog(
            onDismissRequest = { showDeleteCategoryConfirm = null },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
            title = { Text("Eliminar categoría", fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar la categoría \"${cat?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteCategory(catId); showDeleteCategoryConfirm = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCategoryConfirm = null }) { Text("Cancelar") }
            }
        )
    }

    // ── Diálogo: Confirmar eliminar participante ───────────────────────────
    showDeleteParticipantConfirm?.let { participantId ->
        val participant = uiState.participants.firstOrNull { it.id == participantId }
        AlertDialog(
            onDismissRequest = { showDeleteParticipantConfirm = null },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
            title = { Text("Eliminar participante", fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar a \"${participant?.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteParticipant(participantId); showDeleteParticipantConfirm = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteParticipantConfirm = null }) { Text("Cancelar") }
            }
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

// ── Componente card de sección ────────────────────────────────────────────────

@Composable
private fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                    ambientColor = Color.Black.copy(alpha = 0.05f),
                    spotColor = Color.Black.copy(alpha = 0.08f)
                ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

// ── Diálogo: Nueva categoría ──────────────────────────────────────────────────

@Composable
private fun AddCategoryDialog(
    existingCategories: List<Category>,
    onConfirm: (name: String, icon: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name      by rememberSaveable { mutableStateOf("") }
    var icon      by rememberSaveable { mutableStateOf("📦") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = { Text("Nueva categoría", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // ── Categorías ya creadas ──────────────────────────────────
                if (existingCategories.isNotEmpty()) {
                    Text(
                        "Categorías de este grupo:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(existingCategories) { cat ->
                            Surface(
                                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(cat.icon, fontSize = 14.sp)
                                    Text(
                                        cat.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }

                // ── Vista previa + nombre ──────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(JungleGreen100),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(icon, fontSize = 26.sp)
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        label = { Text("Nombre *") },
                        isError = nameError != null,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        colors = appOutlinedTextFieldColors()
                    )
                }

                // ── Selector de emoji ──────────────────────────────────────
                Text(
                    "Elige un icono:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(EMOJI_PICKER) { emoji ->
                        val isSelected = icon == emoji
                        Surface(
                            onClick = { icon = emoji },
                            shape = CircleShape,
                            color = if (isSelected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                nameError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    when {
                        trimmed.isBlank() ->
                            nameError = "El nombre no puede estar vacío"
                        existingCategories.any { it.name.equals(trimmed, ignoreCase = true) } ->
                            nameError = "Ya existe una categoría con ese nombre"
                        else -> onConfirm(trimmed, icon)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White)
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Diálogo: Reparto por defecto ──────────────────────────────────────────────

@Composable
private fun DefaultSplitDialog(
    participants: List<Participant>,
    currentPercentages: Map<Long, Double>,
    onConfirm: (Map<Long, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    // Inicializar con los valores guardados, o reparto equitativo si no hay
    val equalShare = if (participants.isNotEmpty())
        (100.0 / participants.size) else 0.0

    val pctTexts = remember(participants, currentPercentages) {
        mutableStateOf(
            participants.associate { p ->
                p.id to (currentPercentages[p.id]?.fmt2s() ?: equalShare.fmt2s())
            }
        )
    }

    var splitError by rememberSaveable { mutableStateOf<String?>(null) }

    val totalPct by remember(pctTexts.value) {
        derivedStateOf {
            pctTexts.value.values.mapNotNull { it.replace(",", ".").toDoubleOrNull() }.sum()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = { Text("Reparto por defecto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Define el porcentaje que corresponde a cada participante por defecto al crear gastos. Deben sumar 100%.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Indicador total
                Surface(
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                    color = if (kotlin.math.abs(totalPct - 100.0) < 0.01)
                        JungleGreen100
                    else
                        MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (kotlin.math.abs(totalPct - 100.0) < 0.01)
                                JungleGreenDark
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "${totalPct.fmt2s()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (kotlin.math.abs(totalPct - 100.0) < 0.01)
                                JungleGreenDark
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Botón equitativo rápido
                TextButton(
                    onClick = {
                        pctTexts.value = participants.associate { p -> p.id to equalShare.fmt2s() }
                        splitError = null
                    }
                ) {
                    Icon(Icons.Default.Balance, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Distribuir equitativamente", style = MaterialTheme.typography.labelMedium)
                }

                HorizontalDivider()

                participants.forEach { p ->
                    val avatarColor = settingsAvatarPalette[p.name.length % settingsAvatarPalette.size]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                p.name.first().uppercaseChar().toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Text(
                            p.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pctTexts.value[p.id] ?: "",
                            onValueChange = { v ->
                                val edited = v.replace(",", ".").toDoubleOrNull()
                                val others = participants.filter { it.id != p.id }
                                val newMap = if (edited != null && others.isNotEmpty()) {
                                    val remaining = (100.0 - edited).coerceAtLeast(0.0)
                                    val perOther = remaining / others.size
                                    pctTexts.value + (p.id to v) +
                                        others.associate { it.id to perOther.fmt2s() }
                                } else {
                                    pctTexts.value + (p.id to v)
                                }
                                pctTexts.value = newMap
                                splitError = null
                            },
                            label = { Text("%") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(90.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = appOutlinedTextFieldColors()
                        )
                    }
                }

                splitError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (kotlin.math.abs(totalPct - 100.0) >= 0.01) {
                        splitError = "Los porcentajes deben sumar 100% (ahora ${totalPct.fmt2s()}%)"
                        return@Button
                    }
                    val result = participants.associate { p ->
                        p.id to (pctTexts.value[p.id]?.replace(",", ".")?.toDoubleOrNull() ?: 0.0)
                    }
                    onConfirm(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White)
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

