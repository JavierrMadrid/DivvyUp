package com.example.divvyup.integration.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.integration.ui.components.AppFilterChip
import com.example.divvyup.integration.ui.components.AppSearchField
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
import kotlin.time.ExperimentalTime
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
    onLogout: () -> Unit,
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

    LaunchedEffect(Unit) {
        // El ViewModel es compartido en navegación; al volver, refrescamos para reflejar cambios recientes.
        if (uiState.groups.isNotEmpty()) viewModel.loadGroups()
    }

    LaunchedEffect(uiState.createdGroupId) {
        uiState.createdGroupId?.let { groupId ->
            viewModel.consumeNavigation()
            onGroupCreated(groupId)
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (isSelectionMode) "${selectedGroupIds.size} seleccionados"
                            else "DivvyUp",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            if (isSelectionMode) "Mantén pulsado para seleccionar más"
                            else "Tus grupos de gastos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Iconos de usuario y cerrar sesión agrupados sin separación
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenUserSettings) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Ajustes de usuario",
                                tint = if (isAuthenticated) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isAuthenticated) {
                            IconButton(onClick = onLogout) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Cerrar sesión",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                    elevation = 12.dp, shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
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
                        if (isSelectionMode) "Cancelar" else "Nuevo grupo",
                        fontWeight = FontWeight.SemiBold, fontSize = 15.sp
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.groups.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = JungleGreenMid, strokeWidth = 3.dp)
                        Text("Cargando grupos…", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                uiState.groups.isEmpty() -> {
                    EmptyGroupsPlaceholder(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
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
                        getParticipants = { viewModel.getParticipantsForGroup(it) },
                        getCategories = { viewModel.getCategoriesForGroup(it) }
                    )
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
                    action = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
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
            title = { Text("Borrar grupos seleccionados", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "¿Eliminar ${selectedGroupIds.size} grupo(s) seleccionado(s)? " +
                    "Se borrarán todos sus gastos y participantes."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedGroupIds.forEach { viewModel.deleteGroup(it) }
                        selectedGroupIds = emptySet()
                        showDeleteSelectedConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSelectedConfirm = false }) { Text("Cancelar") }
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
        title = { Text("Selecciona un grupo", fontWeight = FontWeight.Bold) },
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalTime::class)
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
    getParticipants: (Long) -> List<Participant>,
    getCategories: (Long) -> List<Category>,
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
                placeholder = "Buscar grupo",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = DivvyUpTokens.ControlHeight)
            )
            Spacer(Modifier.height(4.dp))
        }

        if (groups.isEmpty()) {
            item {
                Text(
                    "No hay grupos que coincidan con la búsqueda",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            return@LazyColumn
        }

        items(groups, key = { it.id }) { group ->
            val isSelected = group.id in selectedGroupIds
            GroupCard(
                group = group,
                isSelected = isSelected,
                onClick = { onGroupClick(group.id) },
                onLongClick = { onGroupLongClick(group.id) },
                onDelete = { onDeleteGroup(group.id) },
                onOpenAdvancedDelete = { onOpenAdvancedDelete(group.id) },
                participants = getParticipants(group.id),
                categories = getCategories(group.id)
            )
        }
    }
}

@OptIn(ExperimentalTime::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
        label = "bgColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(DivvyUpTokens.RadiusCard),
                ambientColor = Color.Black.copy(0.06f),
                spotColor = Color.Black.copy(0.10f))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(DivvyUpTokens.RadiusCard)
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .animateContentSize(),
        shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
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
                        text = "$participantCount participante${if (participantCount == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(DivvyUpTokens.RadiusPill), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(group.currency, style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                }
            }

            // Solo mostrar botones de acción si NO estamos en modo selección
            if (!isSelected) {
                IconButton(onClick = { showDeleteGroupConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar grupo",
                        tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteGroupConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteGroupConfirm = false },
            shape = RoundedCornerShape(DivvyUpTokens.RadiusDialog),
            title = { Text("Eliminar grupo", fontWeight = FontWeight.Bold) },
            text = { Text("¿Eliminar \"${group.name}\"? Se borrarán todos sus gastos y participantes.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteGroupConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteGroupConfirm = false }) { Text("Cancelar") } }
        )
    }
}

// -- Opciones de tiempo para borrado avanzado ----------------------------------
private enum class DeleteTimeOption(val label: String) {
    TODO("Todos los gastos"),
    ANTES_SEMANA("Anteriores a 1 semana"),
    ANTES_MES("Anteriores a 1 mes"),
    ANTES_TRES_MESES("Anteriores a 3 meses"),
    ANTES_ANYO("Anteriores a 1 año")
}

@OptIn(ExperimentalTime::class)
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
                Text("Borrar gastos", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Grupo: $groupName", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // -- Filtro por tiempo --------------------------------------
                Text("Período", style = MaterialTheme.typography.labelMedium,
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
                    Text("Categoría (opcional)", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            val isSel = selectedCategory == null
                            AppFilterChip(
                                label = "Todas",
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
                    Text("Persona (opcional)", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            val isSel = selectedParticipant == null
                            AppFilterChip(
                                label = "Todos",
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
                    Text(
                        buildString {
                            append("Se borrarán los gastos")
                            when (selectedTime) {
                                DeleteTimeOption.TODO             -> append(" (todos)")
                                DeleteTimeOption.ANTES_SEMANA     -> append(" anteriores a la última semana")
                                DeleteTimeOption.ANTES_MES        -> append(" anteriores al último mes")
                                DeleteTimeOption.ANTES_TRES_MESES -> append(" anteriores a los últimos 3 meses")
                                DeleteTimeOption.ANTES_ANYO       -> append(" anteriores al último año")
                            }
                            if (selectedCategory    != null) append(" de la categoría seleccionada")
                            if (selectedParticipant != null) append(" pagados por la persona seleccionada")
                            append(". Esta acción no se puede deshacer.")
                        },
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
            ) { Text("Borrar gastos", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


@Composable
private fun EmptyGroupsPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape)
                .background(brush = Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Groups, contentDescription = null,
                modifier = Modifier.size(48.dp), tint = Color.White)
        }
        Text("Sin grupos todavía", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Crea tu primer grupo para empezar\na compartir gastos con tus amigos",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
    }
}
