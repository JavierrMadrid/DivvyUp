package com.example.divvyup.integration.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.divvyup.application.GroupService
import com.example.divvyup.integration.firebase.FGroupRepository
import com.example.divvyup.integration.ui.navigation.AppNavigation
import com.example.divvyup.integration.ui.theme.DivvyUpTheme
import com.example.divvyup.integration.ui.viewmodel.GroupViewModel
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar dependencias
        val firestore = FirebaseFirestore.getInstance()
        val groupRepository = FGroupRepository(firestore)
        val groupService = GroupService(groupRepository)
        val viewModel = GroupViewModel(groupService)

        setContent {
            DivvyUpTheme {
                DivvyUpApp(viewModel)
            }
        }
    }
}

@Composable
fun DivvyUpApp(viewModel: GroupViewModel) {
    val navController = rememberNavController()

    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
        AppNavigation(
            navController = navController,
            viewModel = viewModel
        )
    }
}