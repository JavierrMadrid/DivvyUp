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
import com.example.divvyup.application.InvitationService
import com.example.divvyup.domain.repository.ParticipantRepository
import com.example.divvyup.domain.repository.ParticipantUserLinkRepository
import com.example.divvyup.integration.ui.screens.AddParticipantInGroupScreen
import com.example.divvyup.integration.ui.screens.ChangePasswordScreen
import com.example.divvyup.integration.ui.screens.AddParticipantsScreen
import com.example.divvyup.integration.ui.screens.AddSpendScreen
import com.example.divvyup.integration.ui.screens.CreateGroupScreen
import com.example.divvyup.integration.ui.screens.GroupDetailScreen
import com.example.divvyup.integration.ui.screens.GroupListScreen
import com.example.divvyup.integration.ui.screens.GroupSettingsScreen
import com.example.divvyup.integration.ui.screens.JoinGroupParticipantScreen
import com.example.divvyup.integration.ui.screens.LoginScreen
import com.example.divvyup.integration.ui.screens.RegisterScreen
import com.example.divvyup.integration.ui.screens.SettleUpScreen
import com.example.divvyup.integration.ui.screens.UserSettingsScreen
import com.example.divvyup.integration.ui.viewmodel.AddParticipantsViewModel
import com.example.divvyup.integration.ui.viewmodel.AuthViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupListViewModel
import com.example.divvyup.integration.ui.viewmodel.JoinGroupParticipantViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    groupListViewModel: GroupListViewModel,
    participantRepository: ParticipantRepository,
    participantUserLinkRepository: ParticipantUserLinkRepository,
    invitationService: InvitationService,
    detailViewModelFactory: (Long) -> GroupDetailViewModel,
    currentUserIdProvider: suspend () -> String?,
    pendingInviteToken: String?,
    consumePendingInviteToken: () -> Unit,
    onShareGroupInvite: (groupId: Long, groupName: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.uiState.collectAsState()
    val detailViewModels = remember { mutableMapOf<Long, GroupDetailViewModel>() }
    val joinViewModels = remember { mutableMapOf<String, JoinGroupParticipantViewModel>() }

    fun getOrCreateDetailVM(groupId: Long) =
        detailViewModels.getOrPut(groupId) { detailViewModelFactory(groupId) }

    fun getOrCreateJoinVM(inviteToken: String?, groupId: Long): JoinGroupParticipantViewModel {
        val key = inviteToken ?: "group-$groupId"
        return joinViewModels.getOrPut(key) {
            JoinGroupParticipantViewModel(
                inviteToken = inviteToken,
                groupIdFallback = groupId,
                invitationService = invitationService,
                currentUserIdProvider = currentUserIdProvider
            )
        }
    }

    // Cuando cambia el estado de autenticación, invalidar caché y recargar grupos
    LaunchedEffect(authState.isAuthenticated, authState.isAnonymous) {
        groupListViewModel.reloadAfterAuthChange()
    }

    // Tras un registro con confirmación pendiente → ir a Login con mensaje informativo
    LaunchedEffect(authState.registrationPendingConfirmation) {
        if (authState.registrationPendingConfirmation) {
            authViewModel.clearRegistrationPending()
            navController.navigate(
                Screen.Login(confirmationMessage = "Te hemos enviado un correo de confirmación. Revisa tu bandeja de entrada para activar tu cuenta.")
            ) {
                popUpTo(Screen.UserSettings) { inclusive = true }
            }
        }
    }

    // Cuando llega una invitación por deep link y el usuario está autenticado, navegar
    LaunchedEffect(authState.isAuthenticated, pendingInviteToken) {
        val token = pendingInviteToken ?: return@LaunchedEffect
        if (!authState.isAuthenticated) return@LaunchedEffect

        consumePendingInviteToken()
        navController.navigate(Screen.JoinGroupParticipant(inviteToken = token, groupId = 0))
    }

    NavHost(
        navController = navController,
        startDestination = Screen.GroupList,
        modifier = modifier
    ) {
        // ── Login ──────────────────────────────────────────────────────────
        composable<Screen.Login> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.Login>()
            LoginScreen(
                viewModel = authViewModel,
                confirmationMessage = route.confirmationMessage,
                onNavigateToRegister = { navController.navigate(Screen.Register) },
                onLoginSuccess = {
                    navController.navigate(Screen.GroupList) {
                        popUpTo(Screen.Login()) { inclusive = true }
                    }
                }
            )
        }

        // ── Registro ───────────────────────────────────────────────────────
        composable<Screen.Register> {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.GroupList) {
                        popUpTo(Screen.Login()) { inclusive = true }
                    }
                }
            )
        }

        // ── Lista de grupos ────────────────────────────────────────────────
        composable<Screen.GroupList> {
            GroupListScreen(
                viewModel = groupListViewModel,
                isAuthenticated = authState.isAuthenticated,
                onGroupClick = { groupId -> navController.navigate(Screen.GroupDetail(groupId)) },
                onGroupCreated = { groupId -> navController.navigate(Screen.AddParticipants(groupId)) },
                onCreateGroup = { navController.navigate(Screen.CreateGroup) },
                onLogout = { authViewModel.logout() },
                onOpenUserSettings = { navController.navigate(Screen.UserSettings) }
            )
        }

        // ── Ajustes de usuario ─────────────────────────────────────────────
        composable<Screen.UserSettings> {
            UserSettingsScreen(
                authViewModel = authViewModel,
                isAuthenticated = authState.isAuthenticated,
                isAnonymous = authState.isAnonymous,
                onNavigateToLogin = { navController.navigate(Screen.Login()) },
                onNavigateToRegister = { navController.navigate(Screen.Register) },
                onNavigateToChangePassword = { navController.navigate(Screen.ChangePassword) },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Cambiar contraseña ─────────────────────────────────────────────
        composable<Screen.ChangePassword> {
            ChangePasswordScreen(
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ── Crear grupo (nueva pantalla) ───────────────────────────────────
        composable<Screen.CreateGroup> {
            val uiState by groupListViewModel.uiState.collectAsState()
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
            val vm = remember(route.groupId) {
                AddParticipantsViewModel(
                    groupId = route.groupId,
                    participantRepository = participantRepository,
                    participantUserLinkRepository = participantUserLinkRepository,
                    currentUserIdProvider = currentUserIdProvider
                )
            }
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
                onOpenSettings = { navController.navigate(Screen.GroupSettings(groupId)) },
                onOpenSettleUp = { navController.navigate(Screen.SettleUp(groupId)) }
            )
        }

        // ── Pantalla de liquidación ──────────────────────────────────────────
        composable<Screen.SettleUp> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.SettleUp>()
            val groupId = route.groupId
            val detailViewModel = remember(groupId) { getOrCreateDetailVM(groupId) }
            SettleUpScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() }
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
                },
                { onShareGroupInvite(groupId, detailViewModel.uiState.value.group?.name ?: "") }
            )
        }

        // ── Unirse a grupo por invitación ───────────────────────────────────
        composable<Screen.JoinGroupParticipant> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.JoinGroupParticipant>()
            val vm = remember(route.inviteToken, route.groupId) {
                getOrCreateJoinVM(route.inviteToken, route.groupId)
            }
            JoinGroupParticipantScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onJoinedGroup = { joinedGroupId ->
                    navController.navigate(Screen.GroupDetail(joinedGroupId)) {
                        popUpTo<Screen.JoinGroupParticipant> { inclusive = true }
                    }
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
