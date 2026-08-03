package com.example.divvyup.integration.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.divvyup.integration.ui.screens.groupsettings.GroupSettingsScreen
import com.example.divvyup.integration.ui.screens.JoinGroupParticipantScreen
import com.example.divvyup.integration.ui.screens.LoginScreen
import com.example.divvyup.integration.ui.screens.RegisterScreen
import com.example.divvyup.integration.ui.screens.SettleUpScreen
import com.example.divvyup.integration.ui.screens.SpendDetailScreen
import com.example.divvyup.integration.ui.screens.UserSettingsScreen
import com.example.divvyup.integration.ui.viewmodel.AddParticipantsViewModel
import com.example.divvyup.integration.ui.viewmodel.AuthViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupListViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupDetailTab
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
    onShareText: (text: String) -> Unit = {},
    onSharePdf: (com.example.divvyup.application.AnalyticsExportData) -> Unit = {},
    onShareExcel: (com.example.divvyup.application.AnalyticsExportData) -> Unit = {},
    /** ID del grupo cuya pestaña Actividad debe abrirse al pulsar una notificación. */
    pendingOpenActivityGroupId: Long? = null,
    consumePendingOpenActivityGroupId: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.uiState.collectAsState()
    val detailViewModels = remember { mutableMapOf<Long, GroupDetailViewModel>() }
    val joinViewModels = remember { mutableMapOf<String, JoinGroupParticipantViewModel>() }

    // Gateo de carga inicial: sólo lanzamos la primera carga de grupos cuando
    // Supabase ya resolvió el estado de sesión persistido. Antes, el `init { loadGroups() }`
    // del VM se ejecutaba durante SessionStatus.Initializing, las queries salían
    // con 401 y la app pintaba "no hay grupos" durante varios segundos. Ahora
    // loadIfReady ignora el trigger hasta que isSessionReady = true.
    LaunchedEffect(authState.isSessionReady) {
        if (authState.isSessionReady) {
            groupListViewModel.loadIfReady(authReady = true)
        }
    }

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

    // ── Transiciones de auth IN-APP (login, logout, upgrade anónimo) ────────
    // Sólo recargamos cuando hay un cambio REAL en la sesión. detailViewModels.clear()
    // es imprescindible para que el próximo usuario obtenga un ViewModel fresco
    // con su propio isOwner/myParticipantId. NO se dispara en cold start (cuando
    // el snapshot inicial es (false, false) y llega (true, *)) porque entonces
    // invalidaríamos la caché antes de que la primera carga del VM termine.
    //
    // La carga inicial la hace `init { loadGroups() }` en GroupListViewModel; aquí
    // sólo reaccionamos a transiciones de auth REALES que ocurran con la app ya
    // mostrando datos (login desde anónimo, logout, etc.).
    val authKey = authState.isAuthenticated to authState.isAnonymous
    var lastAuthSnapshot by remember { mutableStateOf(authKey) }
    LaunchedEffect(authKey) {
        val previous = lastAuthSnapshot
        lastAuthSnapshot = authKey
        // Consideramos "transición real" sólo cuando veníamos de un estado de auth
        // DEFINITIVO (authenticated o anonymous) y vamos a otro. La primera
        // transición desde Initializing (false, false) la gestiona el init del VM.
        val wasAuthed = previous.first || previous.second
        val isAuthed = authKey.first || authKey.second
        if (wasAuthed && isAuthed && previous != authKey) {
            detailViewModels.clear()
            joinViewModels.clear()
            groupListViewModel.reloadAfterAuthChange()
        }
    }

    // Tras un registro con confirmación pendiente → ir a Login con mensaje informativo
    LaunchedEffect(authState.registrationPendingConfirmation) {
        if (authState.registrationPendingConfirmation) {
            authViewModel.clearRegistrationPending()
            navController.navigate(
                Screen.Login(confirmationMessage = "Te hemos enviado un correo de confirmación. Revisa tu bandeja de entrada para activar tu cuenta.")
            ) {
                popUpTo(Screen.Register) { inclusive = true }
            }
        }
    }

    // Cuando llega una invitación por deep link:
    //  - Si autenticado → navegar directo a JoinGroupParticipant
    //  - Si no autenticado → redirigir a Login para que se autentique primero;
    //    cuando vuelva autenticado el efecto se disparará de nuevo y navegará al join screen
    LaunchedEffect(authState.isAuthenticated, authState.isAnonymous, pendingInviteToken) {
        val token = pendingInviteToken ?: return@LaunchedEffect
        if (authState.isAuthenticated) {
            consumePendingInviteToken()
            navController.navigate(Screen.JoinGroupParticipant(inviteToken = token, groupId = 0))
        } else {
            // Redirigir a Login con mensaje explicativo; el token se mantiene en pendingInviteToken
            navController.navigate(
                Screen.Login(confirmationMessage = "Inicia sesión o regístrate para unirte al grupo al que te han invitado.")
            ) {
                launchSingleTop = true
            }
        }
    }

    // Abrir GroupDetail en la pestaña Actividad cuando el usuario toca una notificación
    LaunchedEffect(pendingOpenActivityGroupId) {
        val gId = pendingOpenActivityGroupId ?: return@LaunchedEffect
        consumePendingOpenActivityGroupId()
        getOrCreateDetailVM(gId).selectTab(GroupDetailTab.ACTIVIDAD)
        navController.navigate(Screen.GroupDetail(gId)) {
            launchSingleTop = true
            popUpTo(Screen.GroupList) { inclusive = false }
        }
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
                        popUpTo<Screen.GroupList> { inclusive = false }
                        launchSingleTop = true
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
                        popUpTo<Screen.GroupList> { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Lista de grupos ────────────────────────────────────────────────
        composable<Screen.GroupList> { backStackEntry ->
            GroupListScreen(
                viewModel = groupListViewModel,
                isAuthenticated = authState.isAuthenticated,
                onGroupClick = { groupId ->
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(Screen.GroupDetail(groupId))
                    }
                },
                onGroupCreated = { groupId -> navController.navigate(Screen.AddParticipants(groupId)) },
                onCreateGroup = {
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(Screen.CreateGroup)
                    }
                },
                onOpenUserSettings = {
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(Screen.UserSettings) {
                            launchSingleTop = true
                        }
                    }
                }
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
                onOpenSpend = { spendId ->
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(Screen.SpendDetail(groupId, spendId))
                    }
                },
                onOpenSettings = {
                    // Evitar doble disparo durante la animación de salida del composable:
                    // solo navegamos si este back stack entry sigue siendo el destino activo.
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(Screen.GroupSettings(groupId)) {
                            launchSingleTop = true
                        }
                    }
                },
                onOpenSettleUp = {
                    if (navController.currentBackStackEntry == backStackEntry) {
                        navController.navigate(Screen.SettleUp(groupId))
                    }
                },
                onShareText  = onShareText,
                onSharePdf   = onSharePdf,
                onShareExcel = onShareExcel
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
                onShareInvite = { onShareGroupInvite(groupId, detailViewModel.uiState.value.group?.name ?: "") }
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
                    // Volver hasta GroupDetail (saltando SpendDetail si venimos de ahí)
                    navController.popBackStack(Screen.GroupDetail(groupId), inclusive = false)
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

        // ── Detalle de gasto ───────────────────────────────────────────────
        composable<Screen.SpendDetail> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.SpendDetail>()
            val groupId = route.groupId
            val spendId = route.spendId
            val detailViewModel = remember(groupId) { getOrCreateDetailVM(groupId) }


            SpendDetailScreen(
                spendId = spendId,
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.AddSpend(groupId)) }
            )
        }
    }
}
