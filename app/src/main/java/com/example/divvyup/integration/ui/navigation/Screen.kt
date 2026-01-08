package com.example.divvyup.integration.ui.navigation

sealed class Screen(val route: String) {
    object GroupList : Screen("group_list")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
}

