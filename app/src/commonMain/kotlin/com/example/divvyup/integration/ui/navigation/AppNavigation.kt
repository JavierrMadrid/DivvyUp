package com.example.divvyup.integration.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.divvyup.domain.repository.ParticipantRepository
import com.example.divvyup.integration.ui.screens.AddParticipantInGroupScreen
import com.example.divvyup.integration.ui.screens.AddParticipantsScreen
import com.example.divvyup.integration.ui.screens.AddSpendScreen
import com.example.divvyup.integration.ui.screens.CreateGroupScreen
import com.example.divvyup.integration.ui.screens.GroupDetailScreen
import com.example.divvyup.integration.ui.screens.GroupListScreen
import com.example.divvyup.integration.ui.screens.GroupSettingsScreen
import com.example.divvyup.integration.ui.viewmodel.AddParticipantsViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupListViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    groupListViewModel: GroupListViewModel,
    participantRepository: ParticipantRepository,
    detailViewModelFactory: (Long) -> GroupDetailViewModel,
    modifier: Modifier = Modifier
) {
    val detailViewModels = remember { mutableMapOf<Long, GroupDetailViewModel>() }
    fun getOrCreateDetailVM(groupId: Long) =
        detailViewModels.getOrPut(groupId) { detailViewModelFactory(groupId) }

    NavHost(
        navController = navController,
        startDestination = Screen.GroupList,
        modifier = modifier
    ) {
        // ── Lista de grupos ────────────────────────────────────────────────
        composable<Screen.GroupList> {
            GroupListScreen(
                viewModel = groupListViewModel,
                onGroupClick = { groupId -> navController.navigate(Screen.GroupDetail(groupId)) },
                onGroupCreated = { groupId -> navController.navigate(Screen.AddParticipants(groupId)) },
                onCreateGroup = { navController.navigate(Screen.CreateGroup) }
            )
        }

        // ── Crear grupo (nueva pantalla) ───────────────────────────────────
        composable<Screen.CreateGroup> {
            val uiState by groupListViewModel.uiState.collectAsState()
            // Navegar a AddParticipants cuando el grupo se haya creado correctamente
            LaunchedEffect(uiState.createdGroupId) {
                uiState.createdGroupId?.let { groupId ->
                    groupListViewModel.consumeNavigation()
                    navController.navigate(Screen.AddParticipants(groupId)) {
                        popUpTo<Screen.CreateGroup> { inclusive = true }
                    }
                }
            }
            CreateGroupScreen(viewModel = groupListViewModel, onBack = { navController.popBackStack() })
        }

        // ── Añadir participantes (paso 2 al crear grupo) ───────────────────
        composable<Screen.AddParticipants> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.AddParticipants>()
            val vm = remember(route.groupId) { AddParticipantsViewModel(route.groupId, participantRepository) }
            AddParticipantsScreen(
                viewModel = vm,
                onNavigateToDetail = { id ->
                    navController.navigate(Screen.GroupDetail(id)) {
                        popUpTo<Screen.AddParticipants> { inclusive = true }
                    }
                }
            )
        }

        // ── Detalle de grupo ───────────────────────────────────────────────
        composable<Screen.GroupDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.GroupDetail>()
            val groupId = route.groupId
            val detailViewModel = remember(groupId) { getOrCreateDetailVM(groupId) }
            val uiState by detailViewModel.uiState.collectAsState()

            // Reaccionar a la señal de navegar a AddSpendScreen (nuevo o edición)
            LaunchedEffect(uiState.navigateToSpendScreen) {
                if (uiState.navigateToSpendScreen) {
                    detailViewModel.consumeNavigateToSpendScreen()
                    navController.navigate(Screen.AddSpend(groupId))
                }
            }

            GroupDetailScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
                onAddSpend = { detailViewModel.prepareNewSpend() },
                onOpenSettings = { navController.navigate(Screen.GroupSettings(groupId)) }
            )
        }

        // ── Ajustes del grupo ──────────────────────────────────────────────
        composable<Screen.GroupSettings> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.GroupSettings>()
            val groupId = route.groupId
            val detailViewModel = remember(groupId) { getOrCreateDetailVM(groupId) }
            GroupSettingsScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAddParticipant = {
                    navController.navigate(Screen.AddParticipantInGroup(groupId))
                }
            )
        }

        // ── Añadir / Editar gasto ──────────────────────────────────────────
        composable<Screen.AddSpend> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.AddSpend>()
            val groupId = route.groupId
            val detailViewModel = remember(groupId) { getOrCreateDetailVM(groupId) }
            val uiState by detailViewModel.uiState.collectAsState()

            DisposableEffect(backStackEntry, detailViewModel) {
                onDispose {
                    if (backStackEntry.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                        detailViewModel.clearSpendEdit()
                    }
                }
            }

            // Volver cuando el gasto se haya guardado
            LaunchedEffect(uiState.spendSaved) {
                if (uiState.spendSaved) {
                    navController.popBackStack()
                    detailViewModel.consumeSpendSaved()
                }
            }

            AddSpendScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Añadir participante desde detalle (nueva pantalla) ─────────────
        composable<Screen.AddParticipantInGroup> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.AddParticipantInGroup>()
            val groupId = route.groupId
            val detailViewModel = remember(groupId) { getOrCreateDetailVM(groupId) }
            AddParticipantInGroupScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
