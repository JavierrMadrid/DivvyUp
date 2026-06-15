package com.example.divvyup

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.navigation.compose.rememberNavController
import com.example.divvyup.application.ActivityLogService
import com.example.divvyup.application.AnalyticsExportData
import com.example.divvyup.integration.export.ExportShareHelper
import com.example.divvyup.application.CategoryService
import com.example.divvyup.application.GroupService
import com.example.divvyup.application.InvitationService
import com.example.divvyup.application.SettlementService
import com.example.divvyup.application.SpendService
import com.example.divvyup.integration.cache.CachedActivityLogRepository
import com.example.divvyup.integration.cache.CachedCategoryRepository
import com.example.divvyup.integration.cache.CachedGroupRepository
import com.example.divvyup.integration.cache.CachedParticipantRepository
import com.example.divvyup.integration.cache.CachedSettlementRepository
import com.example.divvyup.integration.cache.CachedSpendRepository
import com.example.divvyup.domain.model.ActivityEventType
import com.example.divvyup.integration.notification.SpendNotificationEvent
import com.example.divvyup.integration.notification.SpendNotificationService
import com.example.divvyup.integration.ui.theme.NotificationPreferenceHolder
import com.example.divvyup.integration.supabase.SupabaseActivityLogRepository
import com.example.divvyup.integration.supabase.SupabaseCategoryRepository
import com.example.divvyup.integration.supabase.SupabaseGroupRepository
import com.example.divvyup.integration.supabase.SupabaseInviteTokenRepository
import com.example.divvyup.integration.supabase.SupabaseParticipantRepository
import com.example.divvyup.integration.supabase.SupabaseParticipantUserLinkRepository
import com.example.divvyup.integration.supabase.SupabaseSettlementRepository
import com.example.divvyup.integration.supabase.SupabaseSpendRepository
import com.example.divvyup.integration.supabase.SupabaseStorageService
import com.example.divvyup.integration.supabase.SupabaseUserProfileRepository
import com.example.divvyup.integration.ui.auth.AndroidGoogleSignInHandler
import com.example.divvyup.integration.ui.auth.AndroidSessionManager
import com.example.divvyup.integration.ui.navigation.AppNavigation
import com.example.divvyup.integration.ui.theme.DivvyUpTheme
import com.example.divvyup.integration.ui.theme.ThemeMode
import com.example.divvyup.integration.ui.theme.ThemePreferenceHolder
import com.example.divvyup.integration.ui.viewmodel.AuthViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel
import com.example.divvyup.integration.ui.viewmodel.GroupListViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import androidx.core.content.edit

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var invitationService: InvitationService
    private val pendingInviteToken = MutableStateFlow<String?>(null)
    private val pendingActivityGroup = MutableStateFlow<Long?>(null)
    private var remoteSpendNotificationJob: Job? = null
    private val lastSeenActivityLogIdByGroup = mutableMapOf<Long, Long>()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Restaurar preferencia de tema persistida ───────────────────────
        val prefs = getSharedPreferences("divvyup_prefs", MODE_PRIVATE)
        val savedTheme = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
        ThemePreferenceHolder.setThemeMode(
            runCatching { ThemeMode.valueOf(savedTheme ?: "") }.getOrDefault(ThemeMode.SYSTEM)
        )
        NotificationPreferenceHolder.setSpendNotificationsEnabled(
            prefs.getBoolean("spend_notifications_enabled", true)
        )

        requestNotificationPermissionIfNeeded()
        SpendNotificationService.createChannel(this)
        val rawSpendNotifier = SpendNotificationService.buildNotifier(this)
        val spendNotifier = com.example.divvyup.integration.notification.SpendNotifier { event ->
            if (NotificationPreferenceHolder.spendNotificationsEnabled.value) {
                rawSpendNotifier.notify(event)
            }
        }

        // Persistir cambios futuros en SharedPreferences
        ThemePreferenceHolder.themeMode
            .onEach { mode -> prefs.edit { putString("theme_mode", mode.name) } }
            .launchIn(CoroutineScope(Dispatchers.Main))
        NotificationPreferenceHolder.spendNotificationsEnabled
            .onEach { enabled -> prefs.edit {putBoolean("spend_notifications_enabled", enabled) } }
            .launchIn(CoroutineScope(Dispatchers.Main))

        supabaseClient = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
            install(Auth) {
                sessionManager = AndroidSessionManager(this@MainActivity)
            }
            install(Storage)
        }

        val postgrest = supabaseClient.postgrest
        val auth = supabaseClient.auth

        val groupRepository = CachedGroupRepository(SupabaseGroupRepository(postgrest))
        val participantRepository = CachedParticipantRepository(SupabaseParticipantRepository(postgrest))
        val categoryRepository = CachedCategoryRepository(SupabaseCategoryRepository(postgrest))
        val spendRepository = CachedSpendRepository(SupabaseSpendRepository(postgrest))
        val settlementRepository = CachedSettlementRepository(SupabaseSettlementRepository(postgrest))
        val participantUserLinkRepo  = SupabaseParticipantUserLinkRepository(postgrest)
        val inviteTokenRepository    = SupabaseInviteTokenRepository(postgrest)

        val groupService      = GroupService(groupRepository)
        val spendService      = SpendService(spendRepository)
        val settlementService = SettlementService(
            settlementRepository  = settlementRepository,
            spendRepository       = spendRepository,
            participantRepository = participantRepository,
            categoryRepository    = categoryRepository
        )
        val categoryService = CategoryService(categoryRepository)
        val userProfileRepository = SupabaseUserProfileRepository(postgrest)
        val storageService = SupabaseStorageService(supabaseClient)
        val activityLogService = ActivityLogService(
            CachedActivityLogRepository(SupabaseActivityLogRepository(postgrest))
        )
        invitationService   = InvitationService(
            groupRepository               = groupRepository,
            participantRepository         = participantRepository,
            participantUserLinkRepository = participantUserLinkRepo,
            inviteTokenRepository         = inviteTokenRepository
        )
        startRemoteSpendNotificationPolling(
            groupService = groupService,
            activityLogService = activityLogService,
            participantUserLinkRepo = participantUserLinkRepo,
            spendNotifier = spendNotifier
        )

        setContent {
            DivvyUpTheme {
                val navController = rememberNavController()
                val vm = remember {
                    AuthViewModel(
                        auth = auth,
                        googleSignInHandler = AndroidGoogleSignInHandler(
                            context = this@MainActivity,
                            webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                        ),
                        onAnonymousMigration = { oldUserId, newUserId ->
                            participantUserLinkRepo.migrateAnonymousLinks(oldUserId, newUserId)
                        },
                        userProfileRepository = userProfileRepository,
                        storageService = storageService
                    )
                }
                authViewModel = vm
                val pendingToken by pendingInviteToken.collectAsState()
                val pendingActivityGroupId by pendingActivityGroup.collectAsState()

                val groupListViewModel = remember {
                    GroupListViewModel(
                        groupService          = groupService,
                        spendService          = spendService,
                        participantRepository = participantRepository,
                        categoryService       = categoryService,
                        currentUserIdProvider = {
                            supabaseClient.auth.currentSessionOrNull()?.user?.id
                        },
                        cacheInvalidator      = {
                            groupRepository.clearAll()
                            participantRepository.clearAll()
                            spendRepository.clearAll()
                        }
                    )
                }

                AppNavigation(
                    navController        = navController,
                    authViewModel        = vm,
                    groupListViewModel   = groupListViewModel,
                    participantRepository = participantRepository,
                    participantUserLinkRepository = participantUserLinkRepo,
                    invitationService    = invitationService,
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
                            },
                            currentUserIdProvider = {
                                supabaseClient.auth.currentSessionOrNull()?.user?.id
                            },
                            participantUserLinkRepository = participantUserLinkRepo,
                            activityLogService = activityLogService,
                            userProfileRepository = userProfileRepository,
                            storageService = storageService,
                            spendNotifier = spendNotifier
                        )
                    },
                    currentUserIdProvider = {
                        supabaseClient.auth.currentSessionOrNull()?.user?.id
                    },
                    pendingInviteToken       = pendingToken,
                    consumePendingInviteToken = { pendingInviteToken.value = null },
                    onShareGroupInvite       = { groupId, groupName -> startShareInvite(groupId, groupName) },
                    onShareText              = { text -> startShareText(text) },
                    onSharePdf               = { data -> startSharePdf(data) },
                    onShareExcel             = { data -> startShareExcel(data) },
                    pendingOpenActivityGroupId = pendingActivityGroupId,
                    consumePendingOpenActivityGroupId = { pendingActivityGroup.value = null }
                )
            }
        }

        // Si la app se abre desde deep link en cold start
        consumeIncomingIntent(intent)
    }

    override fun onDestroy() {
        remoteSpendNotificationJob?.cancel()
        super.onDestroy()
    }

    private fun startRemoteSpendNotificationPolling(
        groupService: GroupService,
        activityLogService: ActivityLogService,
        participantUserLinkRepo: SupabaseParticipantUserLinkRepository,
        spendNotifier: com.example.divvyup.integration.notification.SpendNotifier
    ) {
        remoteSpendNotificationJob?.cancel()
        remoteSpendNotificationJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                try {
                    if (!NotificationPreferenceHolder.spendNotificationsEnabled.value) {
                        delay(20_000)
                        continue
                    }
                    val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                    if (userId == null) {
                        delay(20_000)
                        continue
                    }

                    val groups = groupService.getAllGroups()
                    groups.forEach { group ->
                        val logs = activityLogService.getActivityLog(group.id)
                            .filter {
                                it.eventType == ActivityEventType.GASTO_CREADO ||
                                    it.eventType == ActivityEventType.GASTO_EDITADO ||
                                    it.eventType == ActivityEventType.GASTO_ELIMINADO
                            }
                            .sortedBy { it.id }

                        if (logs.isEmpty()) return@forEach

                        val newestId = logs.last().id
                        val previousSeenId = lastSeenActivityLogIdByGroup[group.id]
                        if (previousSeenId == null) {
                            // Primer arranque: tomar snapshot para no notificar histórico antiguo.
                            lastSeenActivityLogIdByGroup[group.id] = newestId
                            return@forEach
                        }

                        val myParticipantId = runCatching {
                            participantUserLinkRepo.findParticipantIdByGroupAndUser(group.id, userId)
                        }.getOrNull()

                        logs.asSequence()
                            .filter { it.id > previousSeenId }
                            .forEach { log ->
                                if (myParticipantId != null && log.actorParticipantId == myParticipantId) return@forEach
                                val event = when (log.eventType) {
                                    ActivityEventType.GASTO_CREADO -> SpendNotificationEvent.Activity(
                                        groupId = group.id,
                                        title = "Gasto añadido",
                                        body = log.description,
                                        actorName = log.actorName
                                    )
                                    ActivityEventType.GASTO_EDITADO -> SpendNotificationEvent.Activity(
                                        groupId = group.id,
                                        title = "Gasto editado",
                                        body = log.description.lineSequence().firstOrNull() ?: "Se editó un gasto",
                                        actorName = log.actorName
                                    )
                                    ActivityEventType.GASTO_ELIMINADO -> SpendNotificationEvent.Activity(
                                        groupId = group.id,
                                        title = "Gasto eliminado",
                                        body = log.description,
                                        actorName = log.actorName
                                    )
                                    else -> null
                                }
                                if (event != null) spendNotifier.notify(event)
                            }

                        lastSeenActivityLogIdByGroup[group.id] = newestId
                    }
                } catch (e: Exception) {
                    println("DEBUG MainActivity: Poll notificaciones remotas error - ${e.message}")
                }
                delay(20_000)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (!NotificationPreferenceHolder.spendNotificationsEnabled.value) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        consumeIncomingIntent(intent)
    }

    // ── Intent dispatching ────────────────────────────────────────────────

    private fun consumeIncomingIntent(intent: Intent?) {
        consumeOAuthIntent(intent)
        consumeJoinIntent(intent)
        consumeNotificationIntent(intent)
    }

    private fun consumeNotificationIntent(intent: Intent?) {
        val groupId = intent?.getLongExtra(SpendNotificationService.EXTRA_GROUP_ID, -1L) ?: -1L
        val openTab = intent?.getBooleanExtra(SpendNotificationService.EXTRA_OPEN_ACTIVITY_TAB, false) ?: false
        if (groupId > 0L && openTab) {
            pendingActivityGroup.value = groupId
            println("DEBUG MainActivity: Notificación → abrir pestaña Actividad groupId=$groupId")
        }
    }

    private fun consumeOAuthIntent(intent: Intent?) {
        if (intent?.data == null || !::authViewModel.isInitialized) return
        if (intent.data?.host != "auth-callback") return

        supabaseClient.handleDeeplinks(
            intent = intent,
            onSessionSuccess = {
                println("DEBUG MainActivity: Sesion OAuth importada correctamente")
                authViewModel.checkSession()
            },
            onError = { error ->
                println("DEBUG MainActivity: Error en OAuth deeplink: ${error.message}")
            }
        )
    }

    private fun consumeJoinIntent(intent: Intent?) {
        val data = intent?.data ?: return

        // Acepta el deep link custom (divvyup://join?token=) y el enlace HTTPS de GitHub Pages
        val token: String? = when {
            data.scheme.equals("divvyup", ignoreCase = true) &&
                data.host.equals("join", ignoreCase = true) ->
                data.getQueryParameter("token")

            data.scheme.equals("https", ignoreCase = true) &&
                data.host.equals("javierrmadrid.github.io", ignoreCase = true) ->
                data.getQueryParameter("token")

            else -> return
        }
        pendingInviteToken.value = token ?: return
        println("DEBUG MainActivity: Invitacion recibida token=$token")
    }

    // ── Compartir texto exportado ─────────────────────────────────────────

    private fun startShareText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir resumen de DivvyUp"))
    }

    private fun startSharePdf(data: AnalyticsExportData) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                ExportShareHelper.sharePdf(this@MainActivity, data)
            } catch (e: Exception) {
                println("DEBUG MainActivity: Error exportando PDF: ${e.message}")
                Toast.makeText(this@MainActivity, "Error al generar el PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startShareExcel(data: AnalyticsExportData) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                ExportShareHelper.shareExcel(this@MainActivity, data)
            } catch (e: Exception) {
                println("DEBUG MainActivity: Error exportando Excel: ${e.message}")
                Toast.makeText(this@MainActivity, "Error al generar el Excel", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Compartir enlace ──────────────────────────────────────────────────

    private fun startShareInvite(groupId: Long, groupName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val userId = supabaseClient.auth.currentSessionOrNull()?.user?.id
                    ?: return@launch

                // generateInviteLink devuelve divvyup://join?token=TOKEN
                val deepLink = invitationService.generateInviteLink(groupId, userId)
                val token = deepLink.toUri().getQueryParameter("token") ?: return@launch

                // Construir el enlace HTTPS con nombre del grupo para mejor preview en mensajería
                val encodedName = Uri.encode(groupName)
                val httpsLink = "https://javierrmadrid.github.io/DivvyUp/invite-link/join.html" +
                    "?token=$token" +
                    "&group=$encodedName"

                // Copiar al portapapeles por si acaso
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Enlace DivvyUp", httpsLink))
                Toast.makeText(this@MainActivity, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show()

                val groupLabel = if (groupName.isNotBlank()) "\"$groupName\"" else "mi grupo"
                val shareText = "¡Te invito a unirte a $groupLabel en DivvyUp! 💸\n\n$httpsLink"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                startActivity(Intent.createChooser(shareIntent, "Compartir invitación a DivvyUp"))
            } catch (e: Exception) {
                println("DEBUG MainActivity: Error al generar enlace de invitacion: ${e.message}")
            }
        }
    }
}
