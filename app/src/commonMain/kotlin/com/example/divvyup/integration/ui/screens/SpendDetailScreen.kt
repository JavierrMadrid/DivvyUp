package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Recurrence
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreen100
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

private fun Double.fmtDetail(): String {
    val rounded = kotlin.math.round(this * 100) / 100.0
    val intPart = rounded.toLong()
    val decPart = abs((rounded - intPart) * 100).toLong()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}

@Composable
fun SpendDetailScreen(
    spendId: Long,
    viewModel: GroupDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val spend = uiState.spends.firstOrNull { it.id == spendId }
    val participants = uiState.participants
    val categories = uiState.categories
    val currency = uiState.group?.currency ?: "EUR"

    // Navegar a edición cuando el ViewModel haya cargado las shares
    LaunchedEffect(uiState.navigateToSpendScreen) {
        if (uiState.navigateToSpendScreen) {
            viewModel.consumeNavigateToSpendScreen()
            onEdit()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
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
                    "Detalle del gasto",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                // Botón Editar — solo para gastos que no son liquidaciones
                val isSettlement = spend?.notes?.startsWith("__settlement_id:") == true
                IconButton(
                    onClick = {
                        spend?.let { viewModel.prepareEditSpend(it) }
                    },
                    enabled = spend != null && !isSettlement
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar gasto",
                        tint = if (spend != null && !isSettlement)
                            JungleGreen
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        modifier = Modifier.size(DivvyUpTokens.IconMd)
                    )
                }
            }
        }
    ) { padding ->
        if (spend == null) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = JungleGreen)
            }
            return@Scaffold
        }

        val payer = participants.firstOrNull { it.id == spend.payerId }
        val category = categories.firstOrNull { it.id == spend.categoryId }
        val dateTime = spend.date.toLocalDateTime(TimeZone.currentSystemDefault())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Cabecera: importe + concepto ──────────────────────────────
            Card(
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        4.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard),
                        ambientColor = Color.Black.copy(0.06f),
                        spotColor = Color.Black.copy(0.1f)
                    ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Icono de categoría
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(JungleGreen100),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            category?.icon ?: "📦",
                            fontSize = 26.sp
                        )
                    }
                    Text(
                        spend.concept,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${spend.amount.fmtDetail()} $currency",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (category != null) {
                        Surface(
                            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                category.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // ── Detalles ──────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        4.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard),
                        ambientColor = Color.Black.copy(0.06f),
                        spotColor = Color.Black.copy(0.1f)
                    ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    Text(
                        "Información",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    DetailRow(label = "Pagó", value = payer?.name ?: "Desconocido")
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    DetailRow(
                        label = "Fecha",
                        value = "%02d/%02d/%04d".format(
                            dateTime.day, dateTime.month.number, dateTime.year
                        )
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    DetailRow(
                        label = "Tipo de reparto",
                        value = when (spend.splitType) {
                            SplitType.EQUAL -> "Equitativo"
                            SplitType.PERCENTAGE -> "Por porcentaje"
                            SplitType.CUSTOM -> "Por importe exacto"
                        }
                    )
                    if (spend.recurrence != Recurrence.NONE) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        DetailRow(
                            label = "Repetición",
                            value = when (spend.recurrence) {
                                Recurrence.WEEKLY -> "Semanal"
                                Recurrence.MONTHLY -> "Mensual"
                                else -> ""
                            }
                        )
                    }
                    if (!spend.notes.isBlank() && !spend.notes.startsWith("__settlement_id:")) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        DetailRow(label = "Notas", value = spend.notes)
                    }
                }
            }

            // ── Participantes en el reparto ───────────────────────────────
            if (participants.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            4.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard),
                            ambientColor = Color.Black.copy(0.06f),
                            spotColor = Color.Black.copy(0.1f)
                        ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Participantes",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        val c = MaterialTheme.colorScheme
                        val avatarPalette = listOf(
                            c.primary, c.secondary, c.tertiary,
                            c.primaryContainer, c.secondaryContainer
                        )
                        participants.forEachIndexed { index, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val avatarColor = avatarPalette[p.name.hashCode().mod(avatarPalette.size)]
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(avatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        p.name.first().uppercaseChar().toString(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    p.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (p.id == spend.payerId) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                if (p.id == spend.payerId) {
                                    Surface(
                                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                        color = JungleGreen
                                    ) {
                                        Text(
                                            "Pagó",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            if (index < participants.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // ── Ticket adjunto ────────────────────────────────────────────
            if (spend.receiptUrl != null) {
                Card(
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            4.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard),
                            ambientColor = Color.Black.copy(0.06f),
                            spotColor = Color.Black.copy(0.1f)
                        ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = JungleGreen,
                            modifier = Modifier.size(DivvyUpTokens.IconLg)
                        )
                        Text(
                            "Ticket adjunto",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.5f)
        )
    }
}

