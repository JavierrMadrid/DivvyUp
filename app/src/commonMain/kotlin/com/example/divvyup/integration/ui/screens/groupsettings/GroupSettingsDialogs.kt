package com.example.divvyup.integration.ui.screens.groupsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.integration.ui.screens.fmt2
import com.example.divvyup.integration.ui.theme.*

// Paleta de avatares para dialogs de ajustes
internal val settingsAvatarPalette = listOf(
    JungleGreen, JungleGreenDark, BarkBrown,
    MossGold, Soil, JungleGreenMid,
    BarkBrownDark, Amber
)

// Emojis predefinidos para elegir icono de categoría
internal val EMOJI_PICKER = listOf(
    "🍔", "🍕", "🍣", "🍜", "☕", "🛒", "🚗", "✈️", "🏨", "🎬",
    "🎮", "🎁", "💊", "🏥", "⚽", "🏋️", "📚", "👗", "💇", "🔧",
    "💡", "📱", "🖥️", "🎵", "🍺", "🍷", "🌴", "🏖️", "🎂", "💰",
    "🏠", "🚿", "🧺", "🐾", "🌿", "⛽", "🅿️", "🎨", "🧳", "📦"
)

internal val CURRENCIES =
    listOf("EUR", "USD", "GBP", "JPY", "MXN", "ARS", "CLP", "COP", "BRL", "CHF")

internal fun Double.fmt2s(): String {
    val rounded = kotlin.math.round(this * 100) / 100.0
    val intPart = rounded.toLong()
    val decPart = kotlin.math.abs((rounded - intPart) * 100).toLong()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}

// ---------------------------------------------------------------------------
// SettingsSectionCard
// ---------------------------------------------------------------------------

@Composable
internal fun SettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
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

// ---------------------------------------------------------------------------
// ConfirmDeleteCategoryDialog
// ---------------------------------------------------------------------------

@Composable
internal fun ConfirmDeleteCategoryDialog(
    categoryName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = { Text("Eliminar categoría", fontWeight = FontWeight.Bold) },
        text = { Text("¿Eliminar la categoría \"$categoryName\"?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Eliminar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ---------------------------------------------------------------------------
// ConfirmDeleteParticipantDialog
// ---------------------------------------------------------------------------

@Composable
internal fun ConfirmDeleteParticipantDialog(
    participantName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = { Text("Eliminar participante", fontWeight = FontWeight.Bold) },
        text = { Text("¿Eliminar a \"$participantName\"?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Eliminar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ---------------------------------------------------------------------------
// AddCategoryDialog
// ---------------------------------------------------------------------------

@Composable
internal fun AddCategoryDialog(
    existingCategories: List<Category>,
    onConfirm: (name: String, icon: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("📦") }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = { Text("Nueva categoría", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (existingCategories.isNotEmpty()) {
                    Text(
                        "Categorías de este grupo:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    Text(cat.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(JungleGreen100),
                        contentAlignment = Alignment.Center
                    ) { Text(icon, fontSize = 26.sp) }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = null },
                        label = { Text("Nombre *") },
                        isError = nameError != null,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = appOutlinedTextFieldColors()
                    )
                }
                Text("Elige un icono:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        trimmed.isBlank() -> nameError = "El nombre no puede estar vacío"
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

// ---------------------------------------------------------------------------
// BudgetEditDialog
// ---------------------------------------------------------------------------

@Composable
internal fun BudgetEditDialog(
    category: Category,
    currency: String,
    onConfirm: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(category.budget?.let { it.fmt2() } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(category.icon, fontSize = 20.sp)
                Text("Presupuesto — ${category.name}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Establece un límite mensual para esta categoría. Déjalo vacío para eliminar el presupuesto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = { Text("Presupuesto mensual ($currency)") },
                    placeholder = { Text("Ej: 200") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                    colors = appOutlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = text.trim().replace(",", ".")
                    if (trimmed.isEmpty()) {
                        onConfirm(null)
                    } else {
                        val value = trimmed.toDoubleOrNull()
                        if (value == null || value < 0) error = "Introduce un importe válido"
                        else onConfirm(value)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White)
            ) { Text("Guardar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ---------------------------------------------------------------------------
// DefaultSplitDialog
// ---------------------------------------------------------------------------

@Composable
internal fun DefaultSplitDialog(
    participants: List<Participant>,
    currentPercentages: Map<Long, Double>,
    onConfirm: (Map<Long, Double>) -> Unit,
    onDismiss: () -> Unit
) {
    val equalShare = if (participants.isNotEmpty()) (100.0 / participants.size) else 0.0

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
                val isValid = kotlin.math.abs(totalPct - 100.0) < 0.01
                Surface(
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                    color = if (isValid) JungleGreen100 else MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                            color = if (isValid) JungleGreenDark else MaterialTheme.colorScheme.onErrorContainer)
                        Text("${totalPct.fmt2s()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold,
                            color = if (isValid) JungleGreenDark else MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                TextButton(onClick = {
                    pctTexts.value = participants.associate { p -> p.id to equalShare.fmt2s() }
                    splitError = null
                }) {
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
                            modifier = Modifier.size(34.dp).clip(CircleShape).background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p.name.first().uppercaseChar().toString(), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Text(p.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        OutlinedTextField(
                            value = pctTexts.value[p.id] ?: "",
                            onValueChange = { v ->
                                val edited = v.replace(",", ".").toDoubleOrNull()
                                val others = participants.filter { it.id != p.id }
                                val newMap = if (edited != null && others.isNotEmpty()) {
                                    val remaining = (100.0 - edited).coerceAtLeast(0.0)
                                    val perOther = remaining / others.size
                                    pctTexts.value + (p.id to v) + others.associate { it.id to perOther.fmt2s() }
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
