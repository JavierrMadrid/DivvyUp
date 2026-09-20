package com.example.divvyup.integration.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.integration.ui.SetSystemBarAppearance
import com.example.divvyup.integration.ui.Strings
import com.example.divvyup.integration.ui.components.AppFilterChip
import com.example.divvyup.integration.ui.components.AppSearchField
import com.example.divvyup.integration.ui.components.SkeletonCard
import com.example.divvyup.integration.ui.components.StaggeredAppear
import com.example.divvyup.integration.ui.components.rememberAppFilterChipPalette
import com.example.divvyup.integration.ui.theme.Amber
import com.example.divvyup.integration.ui.theme.BarkBrown
import com.example.divvyup.integration.ui.theme.BarkBrownDark
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.JungleGreenDark
import com.example.divvyup.integration.ui.theme.JungleGreenMid
import com.example.divvyup.integration.ui.theme.MossGold
import com.example.divvyup.integration.ui.theme.Soil
import com.example.divvyup.integration.ui.viewmodel.GroupListViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

// -- Colores avatar — paleta extraída de Color.kt -------------------------------
private val avatarPalette = listOf(
    JungleGreen, JungleGreenDark, JungleGreenMid,
    BarkBrown, BarkBrownDark, MossGold, Amber, Soil
)

@Composable
fun GroupListScreen(
    viewModel: GroupListViewModel,
    isAuthenticated: Boolean,
    onGroupClick: (Long) -> Unit,
    onGroupCreated: (Long) -> Unit,
    onCreateGroup: () -> Unit,
    onOpenUserSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var groupSearchQuery by rememberSaveable { mutableStateOf("") }
    val filteredGroups by remember(uiState.groups, groupSearchQuery) {
        derivedStateOf {
            val query = groupSearchQuery.trim()
            if (query.isEmpty()) uiState.groups
            else uiState.groups.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    // Estado de selección múltiple (local a la pantalla)
    var selectedGroupIds by rememberSaveable { mutableStateOf(emptySet<Long>()) }
    val isSelectionMode = selectedGroupIds.isNotEmpty()

    // Estado del dialog de borrado avanzado global
    var showAdvancedDeleteForGroup by rememberSaveable { mutableStateOf<Long?>(null) }
    // Estado del dialog de confirmar borrado de seleccionados
    var showDeleteSelectedConfirm by rememberSaveable { mutableStateOf(false) }

    // Sin LaunchedEffect que llame a loadGroups() aquí: la carga inicial la gestiona
    // el `onAuthResolved()` desde MainActivity (espera al JWT restaurado en cold
    // start). Antes había un `LaunchedEffect(Unit) { viewModel.loadGroups() }` que
    // duplicaba el disparo y, combinado con auth sin resolver, provocaba 401 que
    // dejaban la UI en empty state hasta matar el proceso.

    // Refresh on app resume — si la app vuelve de background y la pantalla está
    // activa, los datos cacheados pueden estar stale (TTL expirado, cambios en
    // otro dispositivo, etc.). Sin este observer, la UI seguía mostrando datos
    // viejos o el empty state ficticio de la carga fallida inicial.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadIfReady(authReady = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.createdGroupId) {
        uiState.createdGroupId?.let { groupId ->
            viewModel.consumeNavigation()
            onGroupCreated(groupId)
        }
    }

    SetSystemBarAppearance(useDarkIcons = false)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            bottomStart = DivvyUpTokens.RadiusHero,
                            bottomEnd = DivvyUpTokens.RadiusHero
                        )
                    )
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(JungleGreen, JungleGreenDark)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cabecera pulsable: tocar el título abre ajustes de usuario.
                    // Antes era un Text sin onClick → los toques caían sin respuesta
                    // y la sensación era de "pantalla en blanco". Ahora se comporta
                    // igual que el IconButton de la derecha, compartiendo callback.
                    Row(
                        modifier = Modifier
                            .clickable(
                                onClick = onOpenUserSettings,
                                role = androidx.compose.ui.semantics.Role.Button,
                                onClickLabel = Strings.GroupList.A11Y_USER_SETTINGS
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (isSelectionMode) Strings.Common.selectedCount(selectedGroupIds.size)
                                else Strings.GroupList.APP_TITLE_FALLBACK,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                if (isSelectionMode) Strings.GroupList.SUBTITLE_SELECTION
                                else Strings.GroupList.SUBTITLE_DEFAULT,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    // Icono de usuario
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenUserSettings) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = Strings.GroupList.A11Y_USER_SETTINGS,
                                tint = if (isAuthenticated) Color.White
                                       else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // FAB derecho ? "Nuevo grupo" (siempre visible, cancela selección si activa)
            FloatingActionButton(
                onClick = {
                    if (isSelectionMode) selectedGroupIds = emptySet()
                    else onCreateGroup()
                },
                shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                containerColor = if (isSelectionMode) MaterialTheme.colorScheme.surfaceVariant else JungleGreen,
                contentColor = if (isSelectionMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                modifier = Modifier.shadow(
                    elevation = DivvyUpTokens.ElevationFab, shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                    ambientColor = JungleGreen.copy(alpha = 0.25f),
                    spotColor = JungleGreen.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(
                        if (isSelectionMode) Strings.Common.CANCEL else Strings.GroupList.FAB_NEW_GROUP,
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.groups.isEmpty() -> {
                    // Skeleton list en lugar de spinner — al usuario se le muestra
                    // ya la forma de la lista que verá, lo que reduce perceived loading.
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(DivvyUpTokens.ScreenPaddingH),
                        verticalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapMd)
                    ) {
                        repeat(3) {
                            SkeletonCard(
                                modifier = Modifier.fillMaxWidth(),
                                height = 88.dp
                            )
                        }
                    }
                }
                uiState.groups.isEmpty() -> {
                    EmptyGroupsPlaceholder(
                        onCreateGroup = onCreateGroup,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    @OptIn(ExperimentalMaterial3Api::class)
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = viewModel::loadGroups,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        GroupList(
                            groups = filteredGroups,
                            searchQuery = groupSearchQuery,
                            onSearchQueryChange = { groupSearchQuery = it },
                            selectedGroupIds = selectedGroupIds,
                            onGroupClick = { id ->
                                if (isSelectionMode) {
                                    selectedGroupIds = if (id in selectedGroupIds)
                                        selectedGroupIds - id else selectedGroupIds + id
                                } else onGroupClick(id)
                            },
                            onGroupLongClick = { id ->
                                selectedGroupIds = if (id in selectedGroupIds)
                                    selectedGroupIds - id else selectedGroupIds + id
                            },
                            onDeleteGroup = viewModel::deleteGroup,
                            onOpenAdvancedDelete = { showAdvancedDeleteForGroup = it },
                            participantsByGroup = uiState.participantsByGroup,
                            categoriesByGroup = uiState.categoriesByGroup
                        )
                    }
                }
            }


            // Snackbar de error — se auto-descarta después de 6 segundos
            uiState.error?.let { errorMsg ->
                LaunchedEffect(errorMsg) {
                    kotlinx.coroutines.delay(6_000)
                    viewModel.clearError()
                }
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = { TextButton(onClick = viewModel::clearError) { Text(Strings.Common.OK) } }
                ) { Text(errorMsg) }
            }
        }
    }

    // -- Dialog borrado avanzado de gastos (por grupo concreto o seleccionable) -
    showAdvancedDeleteForGroup?.let { targetGroupId ->
        val groups = uiState.groups
        // Si hay más de un grupo y no se especificó uno concreto, mostramos selector de grupo
        if (targetGroupId == -1L && groups.size > 1) {
            GroupSelectorDialog(
                groups = groups,
                onSelect = { showAdvancedDeleteForGroup = it },
                onDismiss = { showAdvancedDeleteForGroup = null }
            )
        } else {
            val resolvedId = if (targetGroupId == -1L) groups.firstOrNull()?.id ?: return@let else targetGroupId
            val groupName  = groups.find { it.id == resolvedId }?.name ?: ""
            AdvancedDeleteDialog(
                groupName    = groupName,
                participants = viewModel.getParticipantsForGroup(resolvedId),
                categories   = viewModel.getCategoriesForGroup(resolvedId),
                onConfirm    = { catId, payerId, beforeInstant ->
                    viewModel.deleteSpendsForGroup(resolvedId, catId, payerId, beforeInstant)
                    showAdvancedDeleteForGroup = null
                },
                onDismiss = { showAdvancedDeleteForGroup = null }
            )
        }
    }

    // -- Dialog confirmar borrar grupos seleccionados --------------------------
    if (showDeleteSelectedConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteSelectedConfirm = false },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
            title = { Text(Strings.GroupList.DELETE_SELECTED_TITLE, fontWeight = FontWeight.Bold) },
            text = {
                Text(Strings.GroupList.deleteConfirmGroupsSelected(selectedGroupIds.size))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedGroupIds.forEach { viewModel.deleteGroup(it) }
                        selectedGroupIds = emptySet()
                        showDeleteSelectedConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(Strings.Common.DELETE, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) { Text(Strings.Common.CANCEL) }
            }
        )
    }
}

// -- Selector de grupo para el borrado avanzado global -------------------------
@Composable
private fun GroupSelectorDialog(
    groups: List<Group>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = { Text(Strings.GroupList.SELECT_GROUP_TITLE, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { group ->
                    TextButton(
                        onClick = { onSelect(group.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(group.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.Common.CANCEL) } }
    )
}

@Composable
private fun GroupList(
    groups: List<Group>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedGroupIds: Set<Long>,
    onGroupClick: (Long) -> Unit,
    onGroupLongClick: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onOpenAdvancedDelete: (Long) -> Unit,
    participantsByGroup: Map<Long, List<Participant>>,
    categoriesByGroup: Map<Long, List<Category>>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AppSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = Strings.GroupList.SEARCH_PLACEHOLDER,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = DivvyUpTokens.ControlHeight)
            )
            Spacer(Modifier.height(4.dp))
        }

        if (groups.isEmpty()) {
            item {
                Text(
                    Strings.GroupList.SEARCH_EMPTY,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            return@LazyColumn
        }

        itemsIndexed(groups, key = { _, group -> group.id }) { index, group ->
            val isSelected = group.id in selectedGroupIds
            StaggeredAppear(index = index) {
                GroupCard(
                    group = group,
                    isSelected = isSelected,
                    onClick = { onGroupClick(group.id) },
                    onLongClick = { onGroupLongClick(group.id) },
                    onDelete = { onDeleteGroup(group.id) },
                    onOpenAdvancedDelete = { onOpenAdvancedDelete(group.id) },
                    participants = participantsByGroup[group.id].orEmpty(),
                    categories = categoriesByGroup[group.id].orEmpty()
                )
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(
    group: Group,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenAdvancedDelete: () -> Unit,
    participants: List<Participant>,
    categories: List<Category>,
    modifier: Modifier = Modifier
) {
    var showDeleteGroupConfirm by rememberSaveable { mutableStateOf(false) }
    val avatarColor = avatarPalette[group.name.length % avatarPalette.size]
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "borderColor"
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(DivvyUpTokens.ElevationCard, RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = MaterialTheme.colorScheme.primary.copy(0.18f),
                spotColor = MaterialTheme.colorScheme.primary.copy(0.24f))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = if (isSelected) Strings.GroupList.A11Y_DESELECT_GROUP else Strings.GroupList.A11Y_OPEN_GROUP,
                onLongClickLabel = if (isSelected) Strings.GroupList.A11Y_DESELECT_GROUP else Strings.GroupList.A11Y_SELECT_GROUP
            )
            .semantics {
                selected = isSelected
            }
            .animateContentSize(),
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circular con inicial o check si seleccionado
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else avatarColor),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(28.dp))
                } else {
                    Text(
                        group.name.first().uppercaseChar().toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold, color = Color.White
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                if (group.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(group.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val participantCount = participants.size
                    Text(
                        text = Strings.GroupList.participantsCount(participantCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(group.currency, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }

            // Solo mostrar botones de acción si NO estamos en modo selección
            if (!isSelected) {
                IconButton(onClick = { showDeleteGroupConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = Strings.GroupList.A11Y_REMOVE_GROUP,
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteGroupConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirm = false },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
            title = { Text(Strings.GroupList.DELETE_GROUP_TITLE, fontWeight = FontWeight.Bold) },
            text = { Text(Strings.GroupList.deleteGroupConfirm(group.name)) },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteGroupConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(Strings.Common.DELETE, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteGroupConfirm = false }) { Text(Strings.Common.CANCEL) } }
        )
    }
}

// -- Opciones de tiempo para borrado avanzado ----------------------------------
private enum class DeleteTimeOption(val label: String) {
    TODO(Strings.GroupList.TIME_ALL),
    ANTES_SEMANA(Strings.GroupList.TIME_LAST_WEEK),
    ANTES_MES(Strings.GroupList.TIME_LAST_MONTH),
    ANTES_TRES_MESES(Strings.GroupList.TIME_LAST_3_MONTHS),
    ANTES_ANYO(Strings.GroupList.TIME_LAST_YEAR)
}

@Composable
private fun AdvancedDeleteDialog(
    groupName: String,
    participants: List<Participant>,
    categories: List<Category>,
    onConfirm: (categoryId: Long?, payerId: Long?, beforeInstant: Instant?) -> Unit,
    onDismiss: () -> Unit
) {
    val chipPalette = rememberAppFilterChipPalette(selectedColor = JungleGreen)
    val chipSelectedColor = chipPalette.selectedColor
    val chipUnselectedColor = chipPalette.unselectedColor
    val chipUnselectedTextColor = chipPalette.unselectedTextColor

    var selectedCategory    by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedParticipant by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedTime        by rememberSaveable { mutableStateOf(DeleteTimeOption.TODO) }

    // Calcula el instante ANTES del cual se borrarán los gastos
    fun computeBeforeInstant(): Instant? {
        val daysBack = when (selectedTime) {
            DeleteTimeOption.ANTES_SEMANA      -> 7
            DeleteTimeOption.ANTES_MES         -> 30
            DeleteTimeOption.ANTES_TRES_MESES  -> 90
            DeleteTimeOption.ANTES_ANYO        -> 365
            DeleteTimeOption.TODO              -> return null
        }
        return Clock.System.now() - daysBack.days
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                Text(Strings.Common.DELETE_SPENDS_TITLE, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(Strings.GroupList.groupNameLabel(groupName), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // -- Filtro por tiempo --------------------------------------
                Text(Strings.Common.PERIOD_LABEL, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DeleteTimeOption.entries.toList()) { opt ->
                        val isSel = selectedTime == opt
                        AppFilterChip(
                            label = opt.label,
                            selected = isSel,
                            selectedColor = MaterialTheme.colorScheme.error,
                            unselectedColor = chipUnselectedColor,
                            unselectedTextColor = chipUnselectedTextColor,
                            onClick = { selectedTime = opt }
                        )
                    }
                }

                HorizontalDivider()

                // -- Filtro por categoría -----------------------------------
                if (categories.isNotEmpty()) {
                    Text(Strings.Common.CATEGORY_OPTIONAL_LABEL, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            val isSel = selectedCategory == null
                            AppFilterChip(
                                label = Strings.Common.ALL_FEMININE,
                                selected = isSel,
                                selectedColor = chipSelectedColor,
                                unselectedColor = chipUnselectedColor,
                                unselectedTextColor = chipUnselectedTextColor,
                                onClick = { selectedCategory = null }
                            )
                        }
                        items(categories) { cat ->
                            val isSel = selectedCategory == cat.id
                            AppFilterChip(
                                label = "${cat.icon} ${cat.name}",
                                selected = isSel,
                                selectedColor = chipSelectedColor,
                                unselectedColor = chipUnselectedColor,
                                unselectedTextColor = chipUnselectedTextColor,
                                onClick = { selectedCategory = if (isSel) null else cat.id }
                            )
                        }
                    }
                }

                // -- Filtro por persona -------------------------------------
                if (participants.isNotEmpty()) {
                    Text(Strings.Common.PERSON_OPTIONAL_LABEL, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            val isSel = selectedParticipant == null
                            AppFilterChip(
                                label = Strings.Common.ALL_MASCULINE,
                                selected = isSel,
                                selectedColor = chipSelectedColor,
                                unselectedColor = chipUnselectedColor,
                                unselectedTextColor = chipUnselectedTextColor,
                                onClick = { selectedParticipant = null }
                            )
                        }
                        items(participants) { p ->
                            val isSel = selectedParticipant == p.id
                            AppFilterChip(
                                label = p.name,
                                selected = isSel,
                                selectedColor = chipSelectedColor,
                                unselectedColor = chipUnselectedColor,
                                unselectedTextColor = chipUnselectedTextColor,
                                onClick = { selectedParticipant = if (isSel) null else p.id }
                            )
                        }
                    }
                }

                // Aviso resumen
                Surface(shape = RoundedCornerShape(DivvyUpTokens.RadiusControl), color = MaterialTheme.colorScheme.errorContainer) {
                    val timeSuffix = when (selectedTime) {
                        DeleteTimeOption.TODO             -> Strings.GroupList.SUMMARY_ALL_SUFFIX
                        DeleteTimeOption.ANTES_SEMANA     -> Strings.GroupList.SUMMARY_LAST_WEEK
                        DeleteTimeOption.ANTES_MES        -> Strings.GroupList.SUMMARY_LAST_MONTH
                        DeleteTimeOption.ANTES_TRES_MESES -> Strings.GroupList.SUMMARY_LAST_3_MONTHS
                        DeleteTimeOption.ANTES_ANYO       -> Strings.GroupList.SUMMARY_LAST_YEAR
                    }
                    Text(
                        Strings.GroupList.deleteSummary(
                            timeSuffix = timeSuffix,
                            hasCategory = selectedCategory != null,
                            hasPerson = selectedParticipant != null
                        ),
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCategory, selectedParticipant, computeBeforeInstant()) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(Strings.Common.DELETE_SPENDS_CONFIRM, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(Strings.Common.CANCEL) } }
    )
}


@Composable
private fun EmptyGroupsPlaceholder(
    onCreateGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(DivvyUpTokens.RadiusHero))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Groups,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(Strings.GroupList.EMPTY_HEADLINE, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(
            Strings.GroupList.EMPTY_SUBTITLE,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        Button(
            onClick = onCreateGroup,
            shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
            colors = ButtonDefaults.buttonColors(
                containerColor = JungleGreen,
                contentColor = Color.White
            ),
            modifier = Modifier.height(DivvyUpTokens.PrimaryButtonHeight)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(DivvyUpTokens.IconMd))
            Spacer(Modifier.width(DivvyUpTokens.GapSm))
            Text(Strings.GroupList.FAB_NEW_GROUP, fontWeight = FontWeight.SemiBold)
        }
    }
}
