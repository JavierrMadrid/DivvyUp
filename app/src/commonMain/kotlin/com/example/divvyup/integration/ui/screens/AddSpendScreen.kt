package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.integration.ui.resolveDefaultCustomAmounts
import com.example.divvyup.integration.ui.resolveDefaultSelectedParticipantIds
import com.example.divvyup.integration.ui.resolveDefaultSplitPercentages
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreen100
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.theme.JungleGreenMid
import com.example.divvyup.integration.ui.theme.appOutlinedTextFieldColors
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import kotlin.math.abs

private const val DEFAULT_UNCATEGORIZED_ICON = "📦"

@Composable
private fun getAvatarColor(seed: String): Color {
    val c = MaterialTheme.colorScheme
    val palette = listOf(c.primary, c.secondary, c.tertiary, c.primaryContainer, c.secondaryContainer)
    return palette[seed.hashCode().mod(palette.size)]
}

private fun Double.fmt(): String {
    val rounded = kotlin.math.round(this * 100) / 100.0
    val intPart = rounded.toLong()
    val decPart = abs((rounded - intPart) * 100).toLong()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}

private fun String.toDoubleDotOrNull(): Double? = replace(",", ".").toDoubleOrNull()

@Composable
fun AddSpendScreen(
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val participants = uiState.participants
    val categories = uiState.categories.filterNot { it.name.equals("Liquidación", ignoreCase = true) }
    val currency = uiState.group?.currency ?: "EUR"

    val isEditMode = uiState.spendToEdit != null
    val editSpend = uiState.spendToEdit
    val editShares = uiState.sharesForEditedSpend

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        val msg = uiState.error
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    var concept by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedPayer by rememberSaveable { mutableStateOf(0L) }
    var selectedCatId by rememberSaveable { mutableStateOf<Long?>(null) }
    var splitMode by rememberSaveable { mutableStateOf(SplitType.EQUAL) }
    var conceptError by rememberSaveable { mutableStateOf(false) }
    var amountError by rememberSaveable { mutableStateOf(false) }
    var splitError by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedParticipants = rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val pctTexts = remember { mutableStateOf(emptyMap<Long, String>()) }
    val customTexts = remember { mutableStateOf(emptyMap<Long, String>()) }
    var customAmountsTouched by rememberSaveable { mutableStateOf(false) }

    val participantIds = remember(participants) { participants.map { it.id } }
    val defaultSplitPercentages = remember(participantIds, uiState.defaultSplitPercentages) {
        resolveDefaultSplitPercentages(
            participantIds = participantIds,
            savedPercentages = uiState.defaultSplitPercentages
        )
    }
    val defaultSelectedParticipants = remember(participantIds, defaultSplitPercentages) {
        resolveDefaultSelectedParticipantIds(
            participantIds = participantIds,
            percentages = defaultSplitPercentages
        )
    }
    val amount = amountText.toDoubleDotOrNull() ?: 0.0

    LaunchedEffect(participantIds, defaultSplitPercentages) {
        if (!isEditMode && participants.isNotEmpty()) {
            if (selectedPayer == 0L) selectedPayer = participants.first().id
            if (selectedParticipants.value.isEmpty()) selectedParticipants.value = defaultSelectedParticipants
            if (pctTexts.value.isEmpty()) {
                pctTexts.value = participantIds.associateWith { id ->
                    (defaultSplitPercentages[id] ?: 0.0).fmt()
                }
            }
            if (customTexts.value.isEmpty()) customTexts.value = participantIds.associateWith { "" }
        }
    }

    LaunchedEffect(participantIds, defaultSplitPercentages, amount, customAmountsTouched, isEditMode) {
        if (!isEditMode && participants.isNotEmpty() && !customAmountsTouched) {
            val defaults = resolveDefaultCustomAmounts(
                totalAmount = amount,
                participantIds = participantIds,
                percentages = defaultSplitPercentages
            )
            customTexts.value = participantIds.associateWith { id ->
                defaults[id]?.takeIf { amount > 0.0 }?.fmt() ?: ""
            }
        }
    }

    LaunchedEffect(editSpend?.id, editShares.size) {
        val spend = editSpend ?: return@LaunchedEffect
        concept = spend.concept
        amountText = spend.amount.fmt()
        selectedPayer = spend.payerId
        selectedCatId = spend.categoryId
        splitMode = spend.splitType

        when (spend.splitType) {
            SplitType.EQUAL -> {
                selectedParticipants.value = if (editShares.isNotEmpty()) {
                    editShares.map { it.participantId }.toSet()
                } else {
                    participants.map { it.id }.toSet()
                }
            }

            SplitType.PERCENTAGE -> {
                pctTexts.value = if (editShares.isNotEmpty()) {
                    editShares.associate { it.participantId to (it.percentage ?: 0.0).fmt() }
                } else {
                    participants.associate { it.id to (100.0 / participants.size.coerceAtLeast(1)).fmt() }
                }
            }

            SplitType.CUSTOM -> {
                customTexts.value = if (editShares.isNotEmpty()) {
                    editShares.associate { it.participantId to it.amount.fmt() }
                } else {
                    participants.associate { it.id to "" }
                }
                customAmountsTouched = true
            }
        }
    }

    LaunchedEffect(participants) {
        if (selectedPayer == 0L && participants.isNotEmpty()) selectedPayer = participants.first().id
    }

    fun submit(parsed: Double) {
        when (splitMode) {
            SplitType.EQUAL -> {
                val ids = selectedParticipants.value.toList()
                if (ids.isEmpty()) {
                    splitError = "Selecciona al menos un participante"
                    return
                }
                if (isEditMode) {
                    viewModel.updateEqualSpend(concept.trim(), parsed, selectedPayer, ids, selectedCatId)
                } else {
                    viewModel.createEqualSpend(concept.trim(), parsed, selectedPayer, ids, selectedCatId)
                }
            }

            SplitType.PERCENTAGE -> {
                val totalPct = pctTexts.value.values.mapNotNull { it.toDoubleDotOrNull() }.sum()
                if (abs(totalPct - 100.0) >= 0.01) {
                    splitError = "Los porcentajes deben sumar 100% (ahora ${totalPct.fmt()}%)"
                    return
                }
                val pcts = pctTexts.value
                    .mapValues { (_, v) -> v.toDoubleDotOrNull() ?: 0.0 }
                    .filter { it.value > 0 }

                if (isEditMode) {
                    viewModel.updatePercentageSpend(concept.trim(), parsed, selectedPayer, pcts, selectedCatId)
                } else {
                    viewModel.createPercentageSpend(concept.trim(), parsed, selectedPayer, pcts, selectedCatId)
                }
            }

            SplitType.CUSTOM -> {
                val totalCustom = customTexts.value.values.mapNotNull { it.toDoubleDotOrNull() }.sum()
                if (abs(totalCustom - parsed) >= 0.01) {
                    splitError = "La suma (${totalCustom.fmt()}) no coincide con el total (${parsed.fmt()})"
                    return
                }
                val amounts = customTexts.value
                    .mapValues { (_, v) -> v.toDoubleDotOrNull() ?: 0.0 }
                    .filter { it.value > 0 }

                if (isEditMode) {
                    viewModel.updateCustomSpend(concept.trim(), parsed, selectedPayer, amounts, selectedCatId)
                } else {
                    viewModel.createCustomSpend(concept.trim(), parsed, selectedPayer, amounts, selectedCatId)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
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
                    text = if (isEditMode) "Editar gasto" else "Añadir gasto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(48.dp))
            }
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            conceptError = concept.isBlank()
                            val parsed = amountText.toDoubleDotOrNull()
                            amountError = parsed == null || parsed <= 0
                            if (conceptError || amountError) return@Button
                            submit(parsed!!)
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DivvyUpTokens.PrimaryButtonHeight)
                            .shadow(
                                elevation = 8.dp,
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
                                if (isEditMode) "Guardar cambios" else "Añadir gasto",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    if (isEditMode) "Editar gasto" else "Nuevo gasto",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    when (splitMode) {
                        SplitType.EQUAL -> "Reparto equitativo"
                        SplitType.PERCENTAGE -> "Reparto por porcentaje"
                        SplitType.CUSTOM -> "Reparto por importe exacto"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            FinTextField(
                label = "Concepto",
                value = concept,
                onValueChange = { concept = it; conceptError = false },
                placeholder = "Ej: Cena en La Tagliatella",
                isError = conceptError,
                errorText = "El concepto es obligatorio"
            )

            FinTextField(
                label = "Importe",
                value = amountText,
                onValueChange = {
                    amountText = it
                    amountError = false
                    splitError = null
                },
                placeholder = "0.00",
                isError = amountError,
                errorText = "Introduce un importe válido",
                keyboardType = KeyboardType.Decimal,
                trailingContent = {
                    Text(
                        text = currency,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = JungleGreenMid
                    )
                }
            )

            if (participants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "¿Quién pagó?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    AddSpendParticipantDropdown(
                        participants = participants,
                        selected = selectedPayer,
                        onSelect = { selectedPayer = it }
                    )
                }
            }

            if (categories.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Categoría",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    AddSpendCategoryPills(
                        categories = categories,
                        selected = selectedCatId,
                        onSelect = { selectedCatId = it }
                    )
                }
            }

            SplitModeSelector(
                splitMode = splitMode,
                onSelect = {
                    splitMode = it
                    splitError = null
                }
            )

            if (splitMode == SplitType.EQUAL && participants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Reparto entre",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                selectedParticipants.value =
                                    if (selectedParticipants.value.size == participants.size) emptySet()
                                    else participants.map { it.id }.toSet()
                            }
                        ) {
                            Text(
                                if (selectedParticipants.value.size == participants.size) "Desmarcar todos" else "Seleccionar todos",
                                style = MaterialTheme.typography.labelMedium,
                                color = JungleGreenMid
                            )
                        }
                    }

                    val equalShare = if (selectedParticipants.value.isNotEmpty() && amount > 0) {
                        amount / selectedParticipants.value.size
                    } else {
                        0.0
                    }

                    participants.forEach { p ->
                        AddSpendParticipantCheckRow(
                            participant = p,
                            checked = p.id in selectedParticipants.value,
                            shareLabel = if (p.id in selectedParticipants.value && amount > 0) {
                                "${equalShare.fmt()} $currency"
                            } else {
                                null
                            },
                            onCheck = { checked ->
                                selectedParticipants.value = if (checked) {
                                    selectedParticipants.value + p.id
                                } else {
                                    selectedParticipants.value - p.id
                                }
                            }
                        )
                    }
                }
            }

            if (splitMode == SplitType.PERCENTAGE && participants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val totalPct = pctTexts.value.values.mapNotNull { it.toDoubleDotOrNull() }.sum()

                    SplitTotalsBadge(
                        title = "Porcentaje por persona",
                        isValid = abs(totalPct - 100.0) < 0.01,
                        text = "Total: ${totalPct.fmt()}%"
                    )

                    participants.forEach { p ->
                        val pct = pctTexts.value[p.id]?.toDoubleDotOrNull() ?: 0.0
                        val share = if (amount > 0 && pct > 0) (amount * pct / 100.0).fmt() else null

                        AddSpendParticipantPctRow(
                            participant = p,
                            pctText = pctTexts.value[p.id] ?: "",
                            shareLabel = share?.let { "$it $currency" },
                            onPctChange = { v ->
                                val edited = v.toDoubleDotOrNull()
                                val others = participants.filter { it.id != p.id }
                                pctTexts.value = if (edited != null && others.isNotEmpty()) {
                                    val remaining = (100.0 - edited).coerceAtLeast(0.0)
                                    val perOther = remaining / others.size
                                    pctTexts.value + (p.id to v) + others.associate { it.id to perOther.fmt() }
                                } else {
                                    pctTexts.value + (p.id to v)
                                }
                                splitError = null
                            }
                        )
                    }
                }
            }

            if (splitMode == SplitType.CUSTOM && participants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val totalCustom = customTexts.value.values.mapNotNull { it.toDoubleDotOrNull() }.sum()

                    SplitTotalsBadge(
                        title = "Importe por persona",
                        isValid = amount > 0 && abs(totalCustom - amount) < 0.01,
                        text = "${totalCustom.fmt()} / ${amount.fmt()} $currency"
                    )

                    participants.forEach { p ->
                        AddSpendParticipantCustomRow(
                            participant = p,
                            amountText = customTexts.value[p.id] ?: "",
                            currency = currency,
                            onAmountChange = { v ->
                                customAmountsTouched = true
                                val edited = v.toDoubleDotOrNull()
                                val others = participants.filter { it.id != p.id }

                                customTexts.value = if (edited != null && amount > 0 && others.isNotEmpty()) {
                                    val remaining = (amount - edited).coerceAtLeast(0.0)
                                    val perOther = remaining / others.size
                                    customTexts.value + (p.id to v) + others.associate { it.id to perOther.fmt() }
                                } else {
                                    customTexts.value + (p.id to v)
                                }
                                splitError = null
                            }
                        )
                    }
                }
            }

            splitError?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SplitModeSelector(
    splitMode: SplitType,
    onSelect: (SplitType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Tipo de reparto", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                SplitType.EQUAL to "Equitativo",
                SplitType.PERCENTAGE to "Porcentaje",
                SplitType.CUSTOM to "Exacto"
            ).forEach { (type, label) ->
                val selected = splitMode == type
                Surface(
                    onClick = { onSelect(type) },
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                    color = if (selected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(DivvyUpTokens.ChipHeight)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitTotalsBadge(
    title: String,
    isValid: Boolean,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = if (isValid) JungleGreen100 else MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                text,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isValid) JungleGreenDark else MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun FinTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean = false,
    errorText: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            isError = isError,
            supportingText = if (isError) ({ Text(errorText) }) else null,
            singleLine = true,
            shape = RoundedCornerShape(DivvyUpTokens.RadiusRow),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DivvyUpTokens.ControlHeight),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
            ),
            trailingIcon = trailingContent,
            colors = appOutlinedTextFieldColors()
        )
    }
}

@Composable
private fun AddSpendParticipantDropdown(
    participants: List<Participant>,
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = participants.firstOrNull { it.id == selected }?.name ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusRow),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DivvyUpTokens.ControlHeight)
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = appOutlinedTextFieldColors()
        )

        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            participants.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name) },
                    onClick = {
                        onSelect(p.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddSpendCategoryPills(
    categories: List<Category>,
    selected: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = JungleGreen
    val activeOnColor = Color.White

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            val isNone = selected == null
            Surface(
                onClick = { onSelect(null) },
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                color = if (isNone) activeColor else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(DivvyUpTokens.ChipHeight)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(DEFAULT_UNCATEGORIZED_ICON, fontSize = 14.sp)
                    Text(
                        "Sin categoría",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isNone) activeOnColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isNone) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        items(categories, key = { it.id }) { cat ->
            val isSelected = selected == cat.id
            Surface(
                onClick = { onSelect(cat.id) },
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                color = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(DivvyUpTokens.ChipHeight)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(cat.icon, fontSize = 14.sp)
                    Text(
                        cat.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) activeOnColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun ParticipantAvatar(name: String) {
    Box(
        modifier = Modifier
            .size(DivvyUpTokens.AvatarSm)
            .clip(CircleShape)
            .background(getAvatarColor(name)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.first().uppercaseChar().toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AddSpendParticipantCheckRow(
    participant: Participant,
    checked: Boolean,
    shareLabel: String?,
    onCheck: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusRow),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ParticipantAvatar(participant.name)
            Spacer(Modifier.width(12.dp))
            Text(
                participant.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            shareLabel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheck,
                colors = CheckboxDefaults.colors(
                    checkedColor = JungleGreenDark,
                    checkmarkColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun AddSpendParticipantPctRow(
    participant: Participant,
    pctText: String,
    shareLabel: String?,
    onPctChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusRow),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ParticipantAvatar(participant.name)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    participant.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                shareLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedTextField(
                value = pctText,
                onValueChange = onPctChange,
                label = { Text("%") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .width(90.dp)
                    .heightIn(min = DivvyUpTokens.ControlHeight),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                colors = appOutlinedTextFieldColors()
            )
        }
    }
}

@Composable
private fun AddSpendParticipantCustomRow(
    participant: Participant,
    amountText: String,
    currency: String,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(DivvyUpTokens.RadiusRow),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ParticipantAvatar(participant.name)
            Text(
                participant.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = onAmountChange,
                label = { Text(currency) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .width(110.dp)
                    .heightIn(min = DivvyUpTokens.ControlHeight),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                colors = appOutlinedTextFieldColors()
            )
        }
    }
}
