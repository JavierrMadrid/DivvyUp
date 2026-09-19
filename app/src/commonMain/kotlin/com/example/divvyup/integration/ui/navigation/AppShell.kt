package com.example.divvyup.integration.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.divvyup.integration.ui.Strings
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import kotlin.reflect.KClass

private data class BottomDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination(Screen.GroupList, Screen.GroupList::class, Strings.Nav.GROUPS, Icons.Default.Groups),
    BottomDestination(Screen.Activity, Screen.Activity::class, Strings.Nav.ACTIVITY, Icons.Default.History),
    BottomDestination(Screen.UserSettings, Screen.UserSettings::class, Strings.Nav.PROFILE, Icons.Default.AccountCircle)
)

/**
 * Shell de la app — barra de navegación inferior tipo pill flotante.
 *
 * Sólo se muestra en los destinos de primer nivel (Grupos / Actividad / Perfil).
 * El contenido (NavHost) recibe el [PaddingValues] para no quedar bajo la barra.
 */
@Composable
fun AppShell(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination != null &&
        bottomDestinations.any { dest -> destination.hierarchy.any { it.hasRoute(dest.routeClass) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                DivvyUpBottomBar(navController = navController, currentDestination = destination)
            }
        },
        content = content
    )
}

@Composable
private fun DivvyUpBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = DivvyUpTokens.ScreenPaddingH, vertical = DivvyUpTokens.GapSm)
    ) {
        Surface(
            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = DivvyUpTokens.ElevationBottomBar,
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DivvyUpTokens.GapSm, vertical = DivvyUpTokens.GapXs),
                horizontalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapXs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomDestinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(dest.routeClass) } == true
                    BottomBarItem(
                        destination = dest,
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(dest.route) {
                                    popUpTo(Screen.GroupList) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    destination: BottomDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(DivvyUpTokens.RadiusCard))
            .clickable(onClick = onClick)
            .padding(vertical = DivvyUpTokens.GapXs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(DivvyUpTokens.RadiusPill))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
                )
                .padding(horizontal = DivvyUpTokens.GapLg, vertical = DivvyUpTokens.GapXs),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = destination.icon,
                contentDescription = destination.label,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else contentColor,
                modifier = Modifier.size(DivvyUpTokens.IconLg)
            )
        }
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = contentColor
        )
    }
}
