package com.example.divvyup.integration.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.divvyup.domain.model.ActivityEventType
import com.example.divvyup.integration.ui.Strings
import com.example.divvyup.integration.ui.components.AppEmptyState
import com.example.divvyup.integration.ui.components.AppTopBar
import com.example.divvyup.integration.ui.components.StaggeredAppear
import com.example.divvyup.integration.ui.components.TopBarVariant
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.ErrorOnContainerDark
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.viewmodel.ActivityFeedItem
import com.example.divvyup.integration.ui.viewmodel.ActivityFeedViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/** Feed de actividad global — agrega los eventos de todos los grupos del usuario. */
@Composable
fun ActivityFeedScreen(
    viewModel: ActivityFeedViewModel,
    onOpenGroup: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = Strings.Activity.TITLE,
                subtitle = Strings.Activity.SUBTITLE,
                variant = TopBarVariant.Gradient
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.items.isEmpty() -> {
                    AppEmptyState(
                        emoji = Strings.Activity.EMOJI_EMPTY,
                        title = Strings.Activity.EMPTY_HEADLINE,
                        body = Strings.Activity.EMPTY_SUBTITLE,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = DivvyUpTokens.ScreenPaddingHLg,
                            vertical = DivvyUpTokens.ScreenPaddingV
                        ),
                        verticalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapSm)
                    ) {
                        itemsIndexed(state.items, key = { _, it -> "${it.log.groupId}-${it.log.id}" }) { index, item ->
                            StaggeredAppear(index = index) {
                                ActivityFeedCard(item = item, onClick = { onOpenGroup(item.log.groupId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityFeedCard(
    item: ActivityFeedItem,
    onClick: () -> Unit
) {
    val entry = item.log
    val (icon, iconBg) = entry.eventType.iconAndColor()
    val iconTint = when (entry.eventType) {
        ActivityEventType.GASTO_ELIMINADO,
        ActivityEventType.LIQUIDACION_ELIMINADA -> ErrorOnContainerDark
        else -> JungleGreenDark
    }
    val lines = entry.description.split("\n")
    val headline = lines.first()
    val changeLines = lines.drop(1).filter { it.isNotBlank() }
    val date = entry.createdAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val dateStr = "${date.day} ${MES_CORTO[date.month.number]} ${date.year} · " +
        "${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}"

    val shape = RoundedCornerShape(DivvyUpTokens.RadiusCard)
    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = DivvyUpTokens.ElevationCard,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DivvyUpTokens.GapMdPlus),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapMd)
        ) {
            Box(
                modifier = Modifier
                    .size(DivvyUpTokens.AvatarMd)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(DivvyUpTokens.IconMd))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    headline,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (changeLines.isNotEmpty()) {
                    Spacer(Modifier.height(DivvyUpTokens.GapXs))
                    changeLines.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapXs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        Strings.Activity.inGroup(item.groupName),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!entry.actorName.isNullOrBlank()) {
                        Text(
                            "· ${Strings.Activity.byActor(entry.actorName)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
