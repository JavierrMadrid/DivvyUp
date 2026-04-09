package com.example.divvyup.integration.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.DebtTransfer
import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreen100
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.theme.JungleGreenMid
import com.example.divvyup.integration.ui.theme.DivvyUpTokens

// --- Tab: Balances -----------------------------------------------------------

@Composable
internal fun BalanceTab(
    balances: List<ParticipantBalance>,
    transfers: List<DebtTransfer>,
    currency: String,
    onLiquidar: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (balances.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(40.dp)
            ) {
                Text("⚖️", fontSize = 48.sp)
                Text("Sin balances todavía", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                Text("Añade gastos para ver cómo se reparte", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Balances individuales", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(balances, key = { it.participantId }) { balance ->
            BalanceCard(balance, currency)
        }
        if (transfers.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Liquidaciones sugeridas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onLiquidar) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Liquidar", fontWeight = FontWeight.SemiBold, color = JungleGreen)
                    }
                }
            }
            items(transfers, key = { "${it.fromParticipantId}-${it.toParticipantId}" }) { transfer ->
                TransferCard(transfer = transfer, currency = currency, isSelectable = false)
            }
        }
    }
}

// --- BalanceCard -------------------------------------------------------------

@Composable
internal fun BalanceCard(
    balance: ParticipantBalance,
    currency: String,
    modifier: Modifier = Modifier
) {
    val isPositive = balance.netBalance >= 0
    val isDark = isSystemInDarkTheme()
    val amountColor = when {
        isPositive && isDark -> MaterialTheme.colorScheme.onSurface
        isPositive           -> JungleGreenMid
        else                 -> MaterialTheme.colorScheme.error
    }
    val avatarColor = participantAvatarPalette[balance.participantName.length % participantAvatarPalette.size]

    Card(
        modifier = modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) {
                Text(balance.participantName.first().uppercaseChar().toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(balance.participantName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val effectiveDebt = if (balance.netBalance < 0) -balance.netBalance else 0.0
                Text("Pagó: ${balance.totalPaid.fmt2()} · Debe: ${effectiveDebt.fmt2()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${if (isPositive) "+" else ""}${balance.netBalance.fmt2()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = amountColor)
                Text(currency, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- TransferCard ------------------------------------------------------------

@Composable
internal fun TransferCard(
    transfer: DebtTransfer,
    currency: String,
    isSelectable: Boolean = false,
    isSelected: Boolean = false,
    onToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(if (isSelected) JungleGreen else Color.Transparent, label = "transferBorder")
    Card(
        onClick = { if (isSelectable) onToggle() },
        modifier = modifier.fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.08f))
            .then(if (isSelectable) Modifier.border(width = if (isSelected) 2.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(DivvyUpTokens.RadiusCard)) else Modifier),
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) JungleGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isSelectable) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(if (isSelected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
            }
            Surface(shape = RoundedCornerShape(DivvyUpTokens.RadiusPill), color = MaterialTheme.colorScheme.errorContainer) {
                Text(transfer.fromName, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Surface(shape = RoundedCornerShape(DivvyUpTokens.RadiusPill), color = JungleGreen100) {
                Text(transfer.toName, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = JungleGreenDark)
            }
            Spacer(Modifier.weight(1f))
            Text("${transfer.amount.fmt2()} $currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isSelected) JungleGreenDark else MaterialTheme.colorScheme.onSurface)
        }
    }
}

// --- Dialog: Liquidar --------------------------------------------------------

@Composable
fun SettleUpDialog(
    transfers: List<DebtTransfer>,
    currency: String,
    onConfirm: (selected: List<DebtTransfer>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedKeys by rememberSaveable {
        mutableStateOf(transfers.map { "${it.fromParticipantId}-${it.toParticipantId}" }.toSet())
    }
    val selectedTransfers = transfers.filter { "${it.fromParticipantId}-${it.toParticipantId}" in selectedKeys }
    val totalSelected = selectedTransfers.sumOf { it.amount }
    val allSelected = selectedKeys.size == transfers.size

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = JungleGreen, modifier = Modifier.size(22.dp))
                Text("Liquidar cuentas", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${selectedKeys.size} de ${transfers.size} seleccionado${if (selectedKeys.size != 1) "s" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = {
                        selectedKeys = if (allSelected) emptySet()
                        else transfers.map { "${it.fromParticipantId}-${it.toParticipantId}" }.toSet()
                    }) {
                        Text(if (allSelected) "Deseleccionar todos" else "Seleccionar todos", style = MaterialTheme.typography.labelMedium, color = JungleGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider()
                if (transfers.isEmpty()) {
                    Surface(shape = RoundedCornerShape(DivvyUpTokens.RadiusControl), color = JungleGreen100, modifier = Modifier.fillMaxWidth()) {
                        Text("✅ ¡Las cuentas están saldadas!", modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = JungleGreenDark)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        transfers.forEach { transfer ->
                            val key = "${transfer.fromParticipantId}-${transfer.toParticipantId}"
                            val isSelected = key in selectedKeys
                            TransferCard(transfer = transfer, currency = currency, isSelectable = true, isSelected = isSelected, onToggle = {
                                selectedKeys = if (isSelected) selectedKeys - key else selectedKeys + key
                            })
                        }
                    }
                }
                if (selectedKeys.isNotEmpty()) {
                    HorizontalDivider()
                    Surface(shape = RoundedCornerShape(DivvyUpTokens.RadiusControl), color = JungleGreen100, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total a liquidar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = JungleGreenDark)
                            Text("${totalSelected.fmt2()} $currency", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = JungleGreenDark)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTransfers) },
                enabled = selectedKeys.isNotEmpty() && transfers.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = JungleGreen, contentColor = Color.White)
            ) { Text("Confirmar liquidación", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


