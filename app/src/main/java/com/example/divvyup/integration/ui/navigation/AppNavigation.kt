package com.example.divvyup.integration.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.divvyup.integration.ui.screens.GroupDetailScreen
import com.example.divvyup.integration.ui.screens.GroupListScreen
import com.example.divvyup.integration.ui.viewmodel.GroupViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    viewModel: GroupViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.GroupList.route
    ) {
        composable(Screen.GroupList.route) {
            val state by viewModel.groupListState.collectAsState()

            GroupListScreen(
                groups = state.groups,
                isLoading = state.isLoading,
                error = state.error,
                onGroupClick = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onCreateGroup = { name ->
                    viewModel.createGroup(name)
                },
                onRefresh = {
                    viewModel.loadGroups()
                }
            )
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val state by viewModel.groupDetailState.collectAsState()

            // Cargar el grupo cuando se navega a esta pantalla
            androidx.compose.runtime.LaunchedEffect(groupId) {
                viewModel.loadGroup(groupId)
            }

            GroupDetailScreen(
                group = state.group,
                isLoading = state.isLoading,
                error = state.error,
                onBackClick = {
                    navController.popBackStack()
                },
                onAddUser = { user ->
                    viewModel.addUserToGroup(groupId, user)
                },
                onRemoveUser = { user ->
                    viewModel.removeUserFromGroup(groupId, user)
                },
                onAddSpend = { spend ->
                    viewModel.addSpend(groupId, spend)
                },
                onRemoveSpend = { spend ->
                    viewModel.removeSpend(groupId, spend)
                }
            )
        }
    }
}

