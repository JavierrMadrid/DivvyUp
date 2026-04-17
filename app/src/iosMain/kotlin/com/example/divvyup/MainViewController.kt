package com.example.divvyup

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import androidx.navigation.compose.rememberNavController
import com.example.divvyup.application.CategoryService
import com.example.divvyup.application.GroupService
import com.example.divvyup.application.InvitationService
import com.example.divvyup.application.SettlementService
import com.example.divvyup.application.SpendService
import com.example.divvyup.integration.supabase.SupabaseCategoryRepository
import com.example.divvyup.integration.supabase.SupabaseGroupRepository
import com.example.divvyup.integration.supabase.SupabaseInviteTokenRepository
import com.example.divvyup.integration.supabase.SupabaseParticipantRepository
import com.example.divvyup.integration.supabase.SupabaseParticipantUserLinkRepository
import com.example.divvyup.integration.supabase.SupabaseSettlementRepository
import com.example.divvyup.integration.supabase.SupabaseSpendRepository
import com.example.divvyup.integration.ui.navigation.AppNavigation
import com.example.divvyup.integration.ui.theme.DivvyUpTheme
import com.example.divvyup.integration.ui.viewmodel.AuthViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupListViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import platform.UIKit.UIViewController

// Punto de entrada iOS — equivalente a MainActivity para Android
// Se llama desde Swift: MainViewControllerKt.MainViewController()
fun MainViewController(
    supabaseUrl: String,
    supabaseAnonKey: String
): UIViewController = ComposeUIViewController {

    // ── Manual DI wiring (igual que MainActivity en Android) ────────────────
    val supabaseClient = remember {
        createSupabaseClient(
            supabaseUrl = supabaseUrl,
            supabaseKey = supabaseAnonKey
        ) {
            install(Postgrest)
            install(Auth)
        }
    }

    val postgrest = supabaseClient.postgrest
    val auth      = supabaseClient.auth

    val groupRepository           = remember { SupabaseGroupRepository(postgrest) }
    val participantRepository     = remember { SupabaseParticipantRepository(postgrest) }
    val categoryRepository        = remember { SupabaseCategoryRepository(postgrest) }
    val spendRepository           = remember { SupabaseSpendRepository(postgrest) }
    val settlementRepository      = remember { SupabaseSettlementRepository(postgrest) }
    val participantUserLinkRepo   = remember { SupabaseParticipantUserLinkRepository(postgrest) }
    val inviteTokenRepository     = remember { SupabaseInviteTokenRepository(postgrest) }

    val groupService      = remember { GroupService(groupRepository) }
    val spendService      = remember { SpendService(spendRepository, participantRepository) }
    val settlementService = remember {
        SettlementService(
            settlementRepository  = settlementRepository,
            spendRepository       = spendRepository,
            participantRepository = participantRepository,
            categoryRepository    = categoryRepository
        )
    }
    val categoryService   = remember { CategoryService(categoryRepository) }
    val invitationService = remember {
        InvitationService(
            groupRepository               = groupRepository,
            participantRepository         = participantRepository,
            participantUserLinkRepository = participantUserLinkRepo,
            inviteTokenRepository         = inviteTokenRepository
        )
    }

    // ── UI ───────────────────────────────────────────────────────────────────
    DivvyUpTheme {
        val navController  = rememberNavController()

        val authViewModel  = remember { AuthViewModel(auth) }

        val groupListViewModel = remember {
            GroupListViewModel(
                groupService          = groupService,
                spendService          = spendService,
                participantRepository = participantRepository,
                categoryService       = categoryService
            )
        }

        AppNavigation(
            navController         = navController,
            authViewModel         = authViewModel,
            groupListViewModel    = groupListViewModel,
            participantRepository = participantRepository,
            participantUserLinkRepository = participantUserLinkRepo,
            invitationService     = invitationService,
            detailViewModelFactory = { groupId ->
                GroupDetailViewModel(
                    groupId               = groupId,
                    groupService          = groupService,
                    spendService          = spendService,
                    settlementService     = settlementService,
                    categoryService       = categoryService,
                    participantRepository = participantRepository,
                    myParticipantIdProvider = {
                        val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                            ?: return@GroupDetailViewModel null
                        participantUserLinkRepo.findParticipantIdByGroupAndUser(groupId, userId)
                    }
                )
            },
            currentUserIdProvider    = { supabaseClient.auth.currentSessionOrNull()?.user?.id },
            pendingInviteToken       = null,   // iOS: gestionar con Universal Links en el futuro
            consumePendingInviteToken = {},
            onShareGroupInvite       = { _, _ -> }      // iOS: implementar con UIActivityViewController si es necesario
        )
    }
}
