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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

    val transfersByDebtor by remember(transfers) {
        derivedStateOf { transfers.groupBy { it.fromParticipantId } }
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
            BalanceCard(
                balance = balance,
                currency = currency,
                debtSubtitle = transfersByDebtor[balance.participantId].toDebtSubtitle(balance)
            )
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

private fun List<DebtTransfer>?.toDebtSubtitle(balance: ParticipantBalance): String {
    if (this.isNullOrEmpty()) {
        return if (balance.netBalance < 0) {
            "Debe: ${(-balance.netBalance).fmt2()}"
        } else {
            "Sin deuda pendiente"
        }
    }
    val totalDebt = this.sumOf { it.amount }
    return if (this.size == 1) {
        "Debe: ${totalDebt.fmt2()} a ${this.first().toName}"
    } else {
        "Debe: ${totalDebt.fmt2()} a ${this.first().toName} y ${this.size - 1} más"
    }
}

// --- BalanceCard -------------------------------------------------------------

@Composable
internal fun BalanceCard(
    balance: ParticipantBalance,
    currency: String,
    debtSubtitle: String,
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
                Text(debtSubtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val isDark = isSystemInDarkTheme()
    val debtorChipContainer = if (isDark) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.errorContainer
    val debtorChipText = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
    val creditorChipContainer = if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else JungleGreen100
    val creditorChipText = if (isDark) MaterialTheme.colorScheme.onSurface else JungleGreenDark
    val amountColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = { if (isSelectable) onToggle() },
        modifier = modifier.fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard), ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.08f))
            .then(if (isSelectable) Modifier.border(width = if (isSelected) 2.dp else 0.dp, color = borderColor, shape = RoundedCornerShape(DivvyUpTokens.RadiusCard)) else Modifier),
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) JungleGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        if (!isSelectable) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(JungleGreen100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                tint = JungleGreenDark,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            "Pago sugerido",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                        color = JungleGreen100
                    ) {
                        Text(
                            text = "${transfer.amount.fmt2()} $currency",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = JungleGreenDark
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        color = debtorChipContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "Debe",
                                style = MaterialTheme.typography.labelSmall,
                                color = debtorChipText.copy(alpha = 0.8f)
                            )
                            Text(
                                transfer.fromName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = debtorChipText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
                        color = creditorChipContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "A",
                                style = MaterialTheme.typography.labelSmall,
                                color = creditorChipText.copy(alpha = 0.8f)
                            )
                            Text(
                                transfer.toName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = creditorChipText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            return@Card
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectable) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(if (isSelected) JungleGreen else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isCompactWidth = maxWidth < 360.dp
                    val maxNameLines = if (isCompactWidth) 2 else 1

                    if (isCompactWidth) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                color = debtorChipContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    transfer.fromName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = debtorChipText,
                                    textAlign = TextAlign.Center,
                                    maxLines = maxNameLines,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                            Surface(
                                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                color = creditorChipContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    transfer.toName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = creditorChipText,
                                    textAlign = TextAlign.Center,
                                    maxLines = maxNameLines,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                color = debtorChipContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    transfer.fromName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = debtorChipText,
                                    textAlign = TextAlign.Center,
                                    maxLines = maxNameLines,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                                color = creditorChipContainer,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    transfer.toName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = creditorChipText,
                                    textAlign = TextAlign.Center,
                                    maxLines = maxNameLines,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Text(
                    "${transfer.amount.fmt2()} $currency",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
