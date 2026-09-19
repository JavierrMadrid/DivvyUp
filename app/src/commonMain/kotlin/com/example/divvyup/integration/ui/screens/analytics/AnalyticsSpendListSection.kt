package com.example.divvyup.integration.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SplitType
import com.example.divvyup.integration.ui.screens.fmt2
import com.example.divvyup.integration.ui.screens.formatLocalDate
import com.example.divvyup.integration.ui.screens.toLocalDate
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.WarningContainer
import com.example.divvyup.integration.ui.theme.WarningOnContainer
import com.example.divvyup.integration.ui.viewmodel.AnalyticsPeriod
import com.example.divvyup.integration.ui.Strings

private const val DEFAULT_UNCATEGORIZED_ICON = "📦"
private val WarningTextColor = WarningOnContainer
private val WarningContainerColor = WarningContainer

@Composable
internal fun AnalyticsSpendList(
    filtered: List<Spend>,
    currency: String,
    categoryMap: Map<Long, Category>,
    participantMap: Map<Long, Participant>,
    searchQuery: String,
    selectedCategories: Set<Long>,
    selectedParticipants: Set<Long>,
    period: AnalyticsPeriod,
    modifier: Modifier = Modifier
) {
    val spendPageSize = 5
    var visibleSpendCount by rememberSaveable(
        filtered.size, searchQuery, selectedCategories, selectedParticipants, period
    ) { mutableStateOf(spendPageSize) }

    val visibleSpends = filtered.take(visibleSpendCount)
    val hasMore = visibleSpendCount < filtered.size

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = Strings.Analytics.listHeader(visibleSpends.size, filtered.size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        visibleSpends.forEach { spend ->
            val category = spend.categoryId?.let { categoryMap[it] }
            val payerName = participantMap[spend.payerId]?.name ?: Strings.Analytics.PAYER_UNKNOWN_FALLBACK
            val dateFormatted = remember(spend.date) { formatLocalDate(spend.date.toLocalDate()) }
            val isNonEqual = spend.splitType != SplitType.EQUAL

            Card(
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCardMd),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(DivvyUpTokens.ElevationCard, RoundedCornerShape(DivvyUpTokens.RadiusCardMd),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(DivvyUpTokens.AvatarMd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = category?.icon ?: DEFAULT_UNCATEGORIZED_ICON, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = spend.concept,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isNonEqual) {
                                Surface(
                                    shape = RoundedCornerShape(DivvyUpTokens.ShapeBadge),
                                    color = WarningContainerColor
                                ) {
                                    Text(
                                        text = when (spend.splitType) {
                                            SplitType.PERCENTAGE -> "%"
                                            SplitType.CUSTOM -> "≠"
                                            SplitType.EQUAL -> "="
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = WarningTextColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = Strings.Analytics.payerAndDate(payerName, dateFormatted),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = Strings.Common.amountCurrency(spend.amount.fmt2(), currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (hasMore) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(
                    onClick = { visibleSpendCount += spendPageSize },
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                    border = BorderStroke(1.dp, JungleGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.ExpandMore, contentDescription = null, tint = JungleGreen, modifier = Modifier.size(DivvyUpTokens.IconSm))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = Strings.Analytics.showMore(filtered.size - visibleSpendCount),
                        color = JungleGreen,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

