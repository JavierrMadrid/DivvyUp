package com.example.divvyup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.example.divvyup.application.CategoryService
import com.example.divvyup.application.GroupService
import com.example.divvyup.application.SettlementService
import com.example.divvyup.application.SpendService
import com.example.divvyup.integration.cache.CachedCategoryRepository
import com.example.divvyup.integration.cache.CachedGroupRepository
import com.example.divvyup.integration.cache.CachedParticipantRepository
import com.example.divvyup.integration.cache.CachedSettlementRepository
import com.example.divvyup.integration.cache.CachedSpendRepository
import com.example.divvyup.integration.supabase.SupabaseCategoryRepository
import com.example.divvyup.integration.supabase.SupabaseGroupRepository
import com.example.divvyup.integration.supabase.SupabaseParticipantRepository
import com.example.divvyup.integration.supabase.SupabaseSettlementRepository
import com.example.divvyup.integration.supabase.SupabaseSpendRepository
import com.example.divvyup.integration.ui.navigation.AppNavigation
import com.example.divvyup.integration.ui.theme.DivvyUpTheme
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupListViewModel
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Manual DI wiring (AGENTS.md — no DI framework) ──────────────────────
        val supabaseClient = createSupabaseClient(
            supabaseUrl    = BuildConfig.SUPABASE_URL,
            supabaseKey    = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
        }

        val postgrest = supabaseClient.postgrest

        // Repositories — envueltos en decoradores de caché en memoria con TTL
        val groupRepository       = CachedGroupRepository(SupabaseGroupRepository(postgrest))
        val participantRepository = CachedParticipantRepository(SupabaseParticipantRepository(postgrest))
        val categoryRepository    = CachedCategoryRepository(SupabaseCategoryRepository(postgrest))
        val spendRepository       = CachedSpendRepository(SupabaseSpendRepository(postgrest))
        val settlementRepository  = CachedSettlementRepository(SupabaseSettlementRepository(postgrest))

        // Services
        val groupService      = GroupService(groupRepository)
        val spendService      = SpendService(spendRepository, participantRepository)
        val settlementService = SettlementService(
            settlementRepository = settlementRepository,
            spendRepository = spendRepository,
            participantRepository = participantRepository,
            categoryRepository = categoryRepository
        )
        val categoryService   = CategoryService(categoryRepository)

        // ── UI ───────────────────────────────────────────────────────────────────
        setContent {
            DivvyUpTheme {
                val navController = rememberNavController()

                val groupListViewModel = remember {
                    GroupListViewModel(
                        groupService          = groupService,
                        spendService          = spendService,
                        participantRepository = participantRepository,
                        categoryService       = categoryService
                    )
                }

                AppNavigation(
                    navController = navController,
                    groupListViewModel = groupListViewModel,
                    participantRepository = participantRepository,
                    detailViewModelFactory = { groupId ->
                        GroupDetailViewModel(
                            groupId            = groupId,
                            groupService       = groupService,
                            spendService       = spendService,
                            settlementService  = settlementService,
                            categoryService    = categoryService,
                            participantRepository = participantRepository
                        )
                    }
                )
            }
        }
    }
}
