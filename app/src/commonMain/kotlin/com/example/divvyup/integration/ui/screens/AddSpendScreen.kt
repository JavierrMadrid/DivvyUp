package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.divvyup.application.CategorySuggestionService
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.Recurrence
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.integration.ui.rememberImagePickerLauncher
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Instant

private const val DEFAULT_UNCATEGORIZED_ICON = "📦"

@Composable
private fun getAvatarColor(seed: String): Color {
    val c = MaterialTheme.colorScheme
    val palette =
        listOf(c.primary, c.secondary, c.tertiary, c.primaryContainer, c.secondaryContainer)
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
    val categories =
        uiState.categories.filterNot { it.name.equals("Liquidación", ignoreCase = true) }
    val currency = uiState.group?.currency ?: "EUR"
    val isEditMode = uiState.spendToEdit != null
    val editSpend = uiState.spendToEdit
    val editShares = uiState.sharesForEditedSpend

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.takeIf { it.isNotBlank() }?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
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
    var userPickedCategory by rememberSaveable { mutableStateOf(false) }
    var suggestedCatName by rememberSaveable { mutableStateOf<String?>(null) }
    var customAmountsTouched by rememberSaveable { mutableStateOf(false) }
    var recurrence by rememberSaveable { mutableStateOf(Recurrence.NONE) }
    var receiptUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var receiptImageAttached by rememberSaveable { mutableStateOf(false) }
    var isUploadingImage by rememberSaveable { mutableStateOf(false) }
    // Fecha del gasto — por defecto hoy
    var selectedDate by rememberSaveable { mutableStateOf(Clock.System.now()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val pickImage = rememberImagePickerLauncher { imageBytes ->
        if (imageBytes != null) {
            isUploadingImage = true
            coroutineScope.launch {
                val url = viewModel.uploadReceiptImage(imageBytes)
                receiptUrl = url
                receiptImageAttached = url != null
                isUploadingImage = false
            }
        }
    }

    val selectedParticipants = rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val pctTexts = remember { mutableStateOf(emptyMap<Long, String>()) }
    val customTexts = remember { mutableStateOf(emptyMap<Long, String>()) }

    val participantIds = remember(participants) { participants.map { it.id } }
    val defaultSplitPercentages = remember(participantIds, uiState.defaultSplitPercentages) {
        resolveDefaultSplitPercentages(participantIds, uiState.defaultSplitPercentages)
    }
    val defaultSelectedParticipants = remember(participantIds, defaultSplitPercentages) {
        resolveDefaultSelectedParticipantIds(participantIds, defaultSplitPercentages)
    }
    val amount = amountText.toDoubleDotOrNull() ?: 0.0

    // Inicialización de valores por defecto (modo creación)
    LaunchedEffect(participantIds, defaultSplitPercentages) {
        if (isEditMode || participants.isEmpty()) return@LaunchedEffect
        if (selectedPayer == 0L) {
            // Pre-seleccionar el participante vinculado al usuario actual; si no, el primero
            val myId = uiState.myParticipantId
            selectedPayer = if (myId != null && participants.any { it.id == myId }) myId
                            else participants.first().id
        }
        if (selectedParticipants.value.isEmpty()) selectedParticipants.value =
            defaultSelectedParticipants
        if (pctTexts.value.isEmpty())
            pctTexts.value =
                participantIds.associateWith { (defaultSplitPercentages[it] ?: 0.0).fmt() }
        if (customTexts.value.isEmpty())
            customTexts.value = participantIds.associateWith { "" }
        // Preseleccionar categoría por defecto del grupo (solo si el usuario no ha elegido ya)
        if (!userPickedCategory && selectedCatId == null) {
            selectedCatId = uiState.group?.defaultCategoryId
        }
    }

    // Actualiza importes custom cuando cambia el total (modo creación)
    LaunchedEffect(
        participantIds,
        defaultSplitPercentages,
        amount,
        customAmountsTouched,
        isEditMode
    ) {
        if (isEditMode || participants.isEmpty() || customAmountsTouched) return@LaunchedEffect
        val defaults = resolveDefaultCustomAmounts(amount, participantIds, defaultSplitPercentages)
        customTexts.value =
            participantIds.associateWith { defaults[it]?.takeIf { amount > 0.0 }?.fmt() ?: "" }
    }

    // Carga datos del gasto a editar
    LaunchedEffect(editSpend?.id, editShares.size) {
        val spend = editSpend ?: return@LaunchedEffect
        concept = spend.concept
        amountText = spend.amount.fmt()
        selectedPayer = spend.payerId
        selectedCatId = spend.categoryId
        splitMode = spend.splitType
        recurrence = spend.recurrence
        receiptUrl = spend.receiptUrl
        receiptImageAttached = spend.receiptUrl != null
        selectedDate = spend.date
        when (spend.splitType) {
            SplitType.EQUAL ->
                selectedParticipants.value = editShares.map { it.participantId }.toSet()
                    .ifEmpty { participants.map { it.id }.toSet() }

            SplitType.PERCENTAGE ->
                pctTexts.value =
                    editShares.associate { it.participantId to (it.percentage ?: 0.0).fmt() }
                        .ifEmpty {
                            participants.associate {
                                it.id to (100.0 / participants.size.coerceAtLeast(
                                    1
                                )).fmt()
                            }
                        }

            SplitType.CUSTOM -> {
                customTexts.value = editShares.associate { it.participantId to it.amount.fmt() }
                    .ifEmpty { participants.associate { it.id to "" } }
                customAmountsTouched = true
            }
        }
    }

    LaunchedEffect(participants) {
        if (selectedPayer == 0L && participants.isNotEmpty()) selectedPayer =
            participants.first().id
    }

    // Autosugerencia de categoría con debounce de 500 ms (solo en modo creación)
    LaunchedEffect(concept) {
        if (userPickedCategory || isEditMode) return@LaunchedEffect
        delay(500)
        val suggested = CategorySuggestionService.suggest(concept)
        suggestedCatName = suggested
        if (suggested != null) {
            categories.firstOrNull { it.name.equals(suggested, ignoreCase = true) }
                ?.let { selectedCatId = it.id }
        }
    }

    // Envío del formulario según tipo de reparto
    fun submit(parsed: Double) {
        val urlTrimmed = receiptUrl?.takeIf { it.isNotBlank() }
        when (splitMode) {
            SplitType.EQUAL -> {
                val ids = selectedParticipants.value.toList()
                if (ids.isEmpty()) {
                    splitError = "Selecciona al menos un participante"; return
                }
                if (isEditMode) viewModel.updateEqualSpend(
                    concept.trim(), parsed, selectedPayer, ids, selectedCatId,
                    recurrence = recurrence, receiptUrl = urlTrimmed, date = selectedDate
                )
                else viewModel.createEqualSpend(
                    concept.trim(), parsed, selectedPayer, ids, selectedCatId,
                    recurrence = recurrence, receiptUrl = urlTrimmed, date = selectedDate
                )
            }

            SplitType.PERCENTAGE -> {
                val totalPct = pctTexts.value.values.mapNotNull { it.toDoubleDotOrNull() }.sum()
                if (abs(totalPct - 100.0) >= 0.01) {
                    splitError =
                        "Los porcentajes deben sumar 100% (ahora ${totalPct.fmt()}%)"; return
                }
                val pcts = pctTexts.value.mapValues { (_, v) -> v.toDoubleDotOrNull() ?: 0.0 }
                    .filter { it.value > 0 }
                if (isEditMode) viewModel.updatePercentageSpend(
                    concept.trim(), parsed, selectedPayer, pcts, selectedCatId,
                    recurrence = recurrence, receiptUrl = urlTrimmed, date = selectedDate
                )
                else viewModel.createPercentageSpend(
                    concept.trim(), parsed, selectedPayer, pcts, selectedCatId,
                    recurrence = recurrence, receiptUrl = urlTrimmed, date = selectedDate
                )
            }

            SplitType.CUSTOM -> {
                val totalCustom =
                    customTexts.value.values.mapNotNull { it.toDoubleDotOrNull() }.sum()
                if (abs(totalCustom - parsed) >= 0.01) {
                    splitError =
                        "La suma (${totalCustom.fmt()}) no coincide con el total (${parsed.fmt()})"; return
                }
                val amounts = customTexts.value.mapValues { (_, v) -> v.toDoubleDotOrNull() ?: 0.0 }
                    .filter { it.value > 0 }
                if (isEditMode) viewModel.updateCustomSpend(
                    concept.trim(), parsed, selectedPayer, amounts, selectedCatId,
                    recurrence = recurrence, receiptUrl = urlTrimmed, date = selectedDate
                )
                else viewModel.createCustomSpend(
                    concept.trim(), parsed, selectedPayer, amounts, selectedCatId,
                    recurrence = recurrence, receiptUrl = urlTrimmed, date = selectedDate
                )
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    if (isEditMode) "Editar gasto" else "Añadir gasto",
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
                    Modifier.fillMaxWidth().navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            conceptError = concept.isBlank()
                            val parsed = amountText.toDoubleDotOrNull()
                            amountError = parsed == null || parsed <= 0
                            if (!conceptError && !amountError) submit(parsed!!)
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(DivvyUpTokens.PrimaryButtonHeight)
                            .shadow(
                                8.dp, RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                ambientColor = JungleGreen.copy(alpha = 0.2f),
                                spotColor = JungleGreen.copy(alpha = 0.35f)
                            ),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = JungleGreen,
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState.isLoading)
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        else
                            Text(
                                if (isEditMode) "Guardar cambios" else "Añadir gasto",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)) {
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

            // ── Concepto + botón de imagen ────────────────────────────────
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapSm)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    FinTextField(
                        label = "Concepto", value = concept, placeholder = "Ej: Cena en La Tagliatella",
                        onValueChange = { concept = it; conceptError = false },
                        isError = conceptError, errorText = "El concepto es obligatorio"
                    )
                }
                // Botón cámara — abre la galería para adjuntar un ticket/recibo
                Box(
                    modifier = Modifier
                        .size(DivvyUpTokens.PrimaryButtonHeight)
                        .shadow(2.dp, RoundedCornerShape(DivvyUpTokens.RadiusControl))
                        .clip(RoundedCornerShape(DivvyUpTokens.RadiusControl))
                        .background(
                            if (receiptImageAttached) JungleGreen
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isUploadingImage -> CircularProgressIndicator(
                            modifier = Modifier.size(DivvyUpTokens.IconMd),
                            strokeWidth = 2.dp,
                            color = if (receiptImageAttached) Color.White else JungleGreen
                        )
                        receiptImageAttached -> IconButton(onClick = pickImage) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Imagen adjunta",
                                tint = Color.White,
                                modifier = Modifier.size(DivvyUpTokens.IconLg)
                            )
                        }
                        else -> IconButton(onClick = pickImage) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Adjuntar imagen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(DivvyUpTokens.IconLg)
                            )
                        }
                    }
                }
            }

            FinTextField(
                label = "Importe", value = amountText, placeholder = "0.00",
                onValueChange = { amountText = it; amountError = false; splitError = null },
                isError = amountError, errorText = "Introduce un importe válido",
                keyboardType = KeyboardType.Decimal,
                trailingContent = {
                    Text(
                        currency,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = JungleGreenMid
                    )
                }
            )

            if (participants.isNotEmpty()) {
                SpendSectionLabel("¿Quién pagó?")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapSm)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AddSpendParticipantDropdown(
                            participants = participants,
                            selected = selectedPayer,
                            onSelect = { selectedPayer = it }
                        )
                    }
                    // Botón selector de fecha
                    SpendDateButton(
                        date = selectedDate,
                        onClick = { showDatePicker = true }
                    )
                }
            }

            // DatePickerDialog
            if (showDatePicker) {
                SpendDatePickerDialog(
                    initialDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    onDismiss = { showDatePicker = false }
                )
            }

            if (categories.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapSm)
                ) {
                    SpendSectionLabel("Categoría")
                    if (suggestedCatName != null && !userPickedCategory && !isEditMode) {
                        val catName = suggestedCatName!!
                        SuggestionChip(
                            onClick = {
                                categories.firstOrNull { it.name.equals(catName, ignoreCase = true) }?.let {
                                    selectedCatId = it.id
                                    userPickedCategory = true
                                    suggestedCatName = null
                                }
                            },
                            label = { Text("💡 $catName", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                AddSpendCategoryPills(
                    categories = categories,
                    selected = selectedCatId,
                    onSelect = { selectedCatId = it; userPickedCategory = true; suggestedCatName = null }
                )
            }

            SplitModeSelector(splitMode) { splitMode = it; splitError = null }

            if (splitMode == SplitType.EQUAL && participants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        SpendSectionLabel("Reparto entre")
                        TextButton(onClick = {
                            selectedParticipants.value =
                                if (selectedParticipants.value.size == participants.size) emptySet()
                                else participants.map { it.id }.toSet()
                        }) {
                            Text(
                                if (selectedParticipants.value.size == participants.size) "Desmarcar todos" else "Seleccionar todos",
                                style = MaterialTheme.typography.labelMedium, color = JungleGreenMid
                            )
                        }
                    }
                    val equalShare =
                        if (selectedParticipants.value.isNotEmpty() && amount > 0) amount / selectedParticipants.value.size else 0.0
                    participants.forEach { p ->
                        AddSpendParticipantCheckRow(
                            participant = p,
                            checked = p.id in selectedParticipants.value,
                            shareLabel = if (p.id in selectedParticipants.value && amount > 0) "${equalShare.fmt()} $currency" else null,
                            onCheck = { checked ->
                                selectedParticipants.value =
                                    if (checked) selectedParticipants.value + p.id else selectedParticipants.value - p.id
                            }
                        )
                    }
                }
            }

            if (splitMode == SplitType.PERCENTAGE && participants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val totalPct = pctTexts.value.values.mapNotNull { it.toDoubleDotOrNull() }.sum()
                    SplitTotalsBadge(
                        "Porcentaje por persona",
                        abs(totalPct - 100.0) < 0.01,
                        "Total: ${totalPct.fmt()}%"
                    )
                    participants.forEach { p ->
                        val pct = pctTexts.value[p.id]?.toDoubleDotOrNull() ?: 0.0
                        AddSpendParticipantPctRow(
                            participant = p,
                            pctText = pctTexts.value[p.id] ?: "",
                            shareLabel = if (amount > 0 && pct > 0) "${(amount * pct / 100.0).fmt()} $currency" else null,
                            onPctChange = { v ->
                                val edited = v.toDoubleDotOrNull()
                                val others = participants.filter { it.id != p.id }
                                pctTexts.value = if (edited != null && others.isNotEmpty()) {
                                    val perOther = (100.0 - edited).coerceAtLeast(0.0) / others.size
                                    pctTexts.value + (p.id to v) + others.associate { it.id to perOther.fmt() }
                                } else pctTexts.value + (p.id to v)
                                splitError = null
                            }
                        )
                    }
                }
            }

            if (splitMode == SplitType.CUSTOM && participants.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val totalCustom =
                        customTexts.value.values.mapNotNull { it.toDoubleDotOrNull() }.sum()
                    SplitTotalsBadge(
                        "Importe por persona",
                        amount > 0 && abs(totalCustom - amount) < 0.01,
                        "${totalCustom.fmt()} / ${amount.fmt()} $currency"
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
                                customTexts.value =
                                    if (edited != null && amount > 0 && others.isNotEmpty()) {
                                        val perOther =
                                            (amount - edited).coerceAtLeast(0.0) / others.size
                                        customTexts.value + (p.id to v) + others.associate { it.id to perOther.fmt() }
                                    } else customTexts.value + (p.id to v)
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
                        Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // ── Recurrencia ───────────────────────────────────────────────
            RecurrenceSelector(recurrence = recurrence, onSelect = { recurrence = it })

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Etiqueta de sección reutilizable dentro del formulario. */
@Composable
private fun SpendSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SplitModeSelector(splitMode: SplitType, onSelect: (SplitType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SpendSectionLabel("Tipo de reparto")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                SplitType.EQUAL to "Equitativo",
                SplitType.PERCENTAGE to "Porcentaje",
                SplitType.CUSTOM to "Exacto"
            )
                .forEach { (type, label) ->
                    val selected = splitMode == type
                    Surface(
                        onClick = { onSelect(type) },
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                        color = if (selected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.height(DivvyUpTokens.ChipHeight)
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp), Alignment.Center) {
                            Text(
                                label, style = MaterialTheme.typography.labelMedium,
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
private fun SplitTotalsBadge(title: String, isValid: Boolean, text: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        SpendSectionLabel(title)
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = if (isValid) JungleGreen100 else MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                text, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
                color = if (isValid) JungleGreenDark else MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun FinTextField(
    label: String, value: String, onValueChange: (String) -> Unit, placeholder: String,
    isError: Boolean = false, errorText: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SpendSectionLabel(label)
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            isError = isError,
            supportingText = if (isError) ({ Text(errorText) }) else null,
            singleLine = true, shape = RoundedCornerShape(DivvyUpTokens.RadiusRow),
            modifier = Modifier.fillMaxWidth().heightIn(min = DivvyUpTokens.ControlHeight),
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
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier) {
        OutlinedTextField(
            value = participants.firstOrNull { it.id == selected }?.name ?: "",
            onValueChange = {}, readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusRow),
            modifier = Modifier.fillMaxWidth().heightIn(min = DivvyUpTokens.ControlHeight)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            colors = appOutlinedTextFieldColors()
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            participants.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name) },
                    onClick = { onSelect(p.id); expanded = false })
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
    LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            CategoryPillSurface(isSelected = selected == null, onClick = { onSelect(null) }) {
                Text(DEFAULT_UNCATEGORIZED_ICON, fontSize = 14.sp)
                Text(
                    "Sin categoría", style = MaterialTheme.typography.labelMedium,
                    color = if (selected == null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected == null) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
        items(categories, key = { it.id }) { cat ->
            CategoryPillSurface(isSelected = selected == cat.id, onClick = { onSelect(cat.id) }) {
                Text(cat.icon, fontSize = 14.sp)
                Text(
                    cat.name, style = MaterialTheme.typography.labelMedium,
                    color = if (selected == cat.id) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected == cat.id) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CategoryPillSurface(
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
        color = if (isSelected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.height(DivvyUpTokens.ChipHeight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@Composable
private fun ParticipantAvatar(name: String) {
    Box(
        Modifier.size(DivvyUpTokens.AvatarSm).clip(CircleShape).background(getAvatarColor(name)),
        Alignment.Center
    ) {
        Text(
            name.first().uppercaseChar().toString(), color = Color.White,
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold
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
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
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
                checked,
                onCheck,
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
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ParticipantAvatar(participant.name)
            Column(Modifier.weight(1f)) {
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
                pctText,
                onPctChange,
                label = { Text("%") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(90.dp).heightIn(min = DivvyUpTokens.ControlHeight),
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
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
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
                amountText,
                onAmountChange,
                label = { Text(currency) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(110.dp).heightIn(min = DivvyUpTokens.ControlHeight),
                shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                colors = appOutlinedTextFieldColors()
            )
        }
    }
}

// ── Selector de recurrencia ────────────────────────────────────────────────────

@Composable
private fun SpendDateButton(date: Instant, onClick: () -> Unit) {
    val localDt = date.toLocalDateTime(TimeZone.currentSystemDefault())
    val label = "${localDt.date.day.toString().padStart(2, '0')}/${localDt.date.month.number.toString().padStart(2, '0')}/${localDt.date.year}"
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusRow),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.height(DivvyUpTokens.ControlHeight)
    ) {
        Icon(
            Icons.Default.CalendarMonth,
            contentDescription = "Seleccionar fecha",
            modifier = Modifier.size(DivvyUpTokens.IconSm),
            tint = JungleGreenMid
        )
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpendDatePickerDialog(
    initialDate: Instant,
    onDateSelected: (Instant) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochMilliseconds()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis
                if (millis != null) {
                    // Ajustar a mediodía UTC para evitar problemas con zona horaria
                    onDateSelected(Instant.fromEpochMilliseconds(millis + 12 * 3600 * 1000L))
                }
                onDismiss()
            }) { Text("Aceptar", color = JungleGreen) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun RecurrenceSelector(recurrence: Recurrence, onSelect: (Recurrence) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SpendSectionLabel("Repetición")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Recurrence.NONE    to "Una vez",
                Recurrence.WEEKLY  to "Semanal",
                Recurrence.MONTHLY to "Mensual"
            ).forEach { (type, label) ->
                val selected = recurrence == type
                Surface(
                    onClick = { onSelect(type) },
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                    color = if (selected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(DivvyUpTokens.ChipHeight)
                ) {
                    Box(Modifier.padding(horizontal = 14.dp), Alignment.Center) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) androidx.compose.ui.graphics.Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (recurrence != Recurrence.NONE) {
            Text(
                when (recurrence) {
                    Recurrence.WEEKLY  -> "💡 Este gasto se repetirá cada semana"
                    Recurrence.MONTHLY -> "💡 Este gasto se repetirá cada mes"
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
