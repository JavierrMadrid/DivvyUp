package com.example.divvyup.integration.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.divvyup.application.CategoryService
import com.example.divvyup.application.GroupService
import com.example.divvyup.application.SpendService
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.repository.ParticipantRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Instant

data class GroupListUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
    val createdGroupId: Long? = null,
    // Caché local para el dialog de borrado avanzado
    val participantsByGroup: Map<Long, List<Participant>> = emptyMap(),
    val categoriesByGroup: Map<Long, List<Category>> = emptyMap()
)

class GroupListViewModel(
    private val groupService: GroupService,
    private val spendService: SpendService,
    private val participantRepository: ParticipantRepository,
    private val categoryService: CategoryService,
    private val currentUserIdProvider: (suspend () -> String?) = { null },
    /** Opcional: invalida la caché subyacente antes de recargar (útil tras cambio de sesión). */
    private val cacheInvalidator: (() -> Unit)? = null
) : ViewModel() {

    private companion object {
        /**
         * Concurrencia máxima al precargar participantes/categorías/spends por grupo.
         * Limita el número de requests HTTP simultáneas para no saturar Ktor ni
         * el rate-limit de Supabase. 8 suele ser seguro con HTTP/2 multiplexing.
         */
        const val MAX_CONCURRENT_GROUP_LOADS = 8
    }

    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()

    /**
     * Guard contra re-entradas concurrentes de loadGroups().
     * Antes disparaba 2-3 invocaciones simultáneas en cold start que provocaban
     * errores 401 transitorios y saturaban la caché. Ahora sólo se ejecuta una vez.
     */
    private var loadInFlight = false

    /**
     * Si `true`, significa que el AuthViewModel ya emitió el primer `sessionStatus`
     * distinto de `Initializing`. Mientras sea `false`, abortamos cualquier carga para
     * evitar tirar queries HTTP sin JWT (devolverían 401 y contaminarían el caché).
     * Tras resolverse auth la primera vez, se queda en `true` para el resto del ciclo
     * del VM (login/logout posteriores usan `reloadAfterAuthChange`).
     */
    private var authResolved = false

    /**
     * Cuando `loadGroups()` se llama antes de que auth esté lista, encolamos la
     * intención aquí. `onAuthResolved` la consumirá y disparará la carga real.
     */
    private var pendingReload = false

    init {
        // Sin carga inicial aquí. Antes se disparaba en cold start antes de que
        // supabase-kt terminase de restaurar el token desde SharedPreferences, lo
        // que provocaba 401 + caché vacío + pantalla en blanco hasta matar el proceso.
        // La carga la dispara `onAuthResolved()` cuando AuthViewModel emite la
        // primera transición real de sessionStatus.
    }

    /**
     * Llamar cuando el AuthViewModel confirma que el sessionStatus ya no es
     * `Initializing`. Dispara la primera carga (si nadie la había encolado) o
     * ejecuta una recarga pendiente que se hubiera solicitado durante el init.
     */
    fun onAuthResolved() {
        if (authResolved) return
        authResolved = true
        if (loadInFlight) return
        if (pendingReload) {
            pendingReload = false
            loadGroups()
        } else {
            loadGroups()
        }
    }

    /**
     * Dispara la carga de grupos sólo si la sesión de Supabase está lista.
     * Si todavía no lo está, encola una recarga para cuando `onAuthResolved`
     * se ejecute. Útil cuando AppNavigation necesita esperar a que auth se
     * resuelva antes de lanzar una recarga (p.ej. tras login in-app).
     * No-op si ya hay una en vuelo.
     */
    fun loadIfReady(authReady: Boolean) {
        if (!authReady) {
            pendingReload = true
            return
        }
        if (!authResolved) {
            // Auth todavía en Initializing: encolar para cuando se resuelva.
            pendingReload = true
            return
        }
        if (loadInFlight) return
        loadGroups()
    }

    fun loadGroups() {
        // Cold start sin auth resuelta: descartar. Si alguien quiere datos, debe
        // llamar a `loadIfReady(true)` o esperar a `onAuthResolved()`. Esto evita
        // tirar queries HTTP sin JWT que revientan el caché y dejan la UI vacía.
        if (!authResolved) {
            pendingReload = true
            return
        }
        if (loadInFlight) return
        loadInFlight = true
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val groups = groupService.getAllGroups()

                // Pintar la lista de inmediato con lo que ya tenemos (sólo 1 query).
                // El precarga de detalle continúa en background y se aplica después.
                _uiState.update {
                    it.copy(
                        groups = groups.sortedWith(
                            compareByDescending<Group> { it.createdAt }
                                .thenByDescending { it.id }
                        ),
                        isLoading = false
                    )
                }

                if (groups.isEmpty()) {
                    _uiState.update { it.copy(groups = emptyList(), isLoading = false) }
                    return@launch
                }
                println("DEBUG GroupListVM: cargados ${groups.size} grupos, precargando detalle en background…")

                // Cargar en paralelo participantes, categorías y última actividad de cada grupo.
                // Semaphore limita concurrencia para no saturar Ktor/Supabase con muchos grupos.
                val semaphore = Semaphore(MAX_CONCURRENT_GROUP_LOADS)
                coroutineScope {
                    val detailJobs = groups.map { group ->
                        async {
                            semaphore.withPermit {
                                // participants y categories en paralelo dentro del grupo
                                val participantsJob = async {
                                    try { participantRepository.getByGroup(group.id) } catch (_: Exception) { emptyList() }
                                }
                                val categoriesJob = async {
                                    try { categoryService.getCategories(group.id) } catch (_: Exception) { emptyList() }
                                }
                                val participants = participantsJob.await()
                                val categories   = categoriesJob.await()
                                // Query ligera (1 fila) en lugar de descargar todos los gastos
                                val lastSpendDate = try { spendService.getLastSpendDate(group.id) } catch (_: Exception) { null }
                                Triple(group, participants, categories) to lastSpendDate
                            }
                        }
                    }

                    // Aplicar cada resultado conforme llega para que contadores/categorías
                    // aparezcan en las tarjetas sin esperar a toda la precarga.
                    val groupLastActivity = mutableMapOf<Long, Instant>()
                    detailJobs.forEach { job ->
                        val (triple, lastSpendDate) = job.await()
                        val (group, participants, categories) = triple
                        groupLastActivity[group.id] = if (lastSpendDate != null && lastSpendDate > group.createdAt) {
                            lastSpendDate
                        } else {
                            group.createdAt
                        }
                        _uiState.update { state ->
                            state.copy(
                                participantsByGroup = state.participantsByGroup + (group.id to participants),
                                categoriesByGroup = state.categoriesByGroup + (group.id to categories)
                            )
                        }
                    }

                    // Reordenar por última actividad una vez completa la precarga.
                    val sortedGroups = groups
                        .sortedWith(compareByDescending<Group> { groupLastActivity[it.id] ?: it.createdAt }
                            .thenByDescending { it.createdAt }
                            .thenByDescending { it.id })

                    _uiState.update { it.copy(groups = sortedGroups) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            } finally {
                loadInFlight = false
            }
        }
    }

    fun getParticipantsForGroup(groupId: Long): List<Participant> =
        _uiState.value.participantsByGroup[groupId] ?: emptyList()

    fun getCategoriesForGroup(groupId: Long): List<Category> =
        _uiState.value.categoriesByGroup[groupId] ?: emptyList()

    fun showCreateDialog() = _uiState.update { it.copy(showCreateDialog = true) }
    fun hideCreateDialog() = _uiState.update { it.copy(showCreateDialog = false) }

    fun createGroup(name: String, description: String, currency: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = currentUserIdProvider()
                if (userId == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Sin conexión a internet. Conéctate para crear grupos.") }
                    return@launch
                }
                val newGroup = groupService.createGroup(name, description, currency, ownerUserId = userId)
                _uiState.update {
                    it.copy(isLoading = false, showCreateDialog = false, createdGroupId = newGroup.id)
                }
                loadGroups()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun consumeNavigation() = _uiState.update { it.copy(createdGroupId = null) }

    /**
     * Invalida la caché de grupos (si existe) y recarga desde Supabase.
     * Llamar tras un cambio de autenticación (login/logout) para que el nuevo
     * usuario vea sus grupos en lugar de los de la sesión anterior.
     * Los grupos se limpian de inmediato para que no queden visibles los del usuario anterior.
     *
     * IMPORTANTE: si ya hay una carga en vuelo, no la cancelamos ni limpiamos el
     * estado — esperamos a que termine y disparamos una recarga adicional sólo si
     * era realmente necesario (p.ej. un cambio de auth in-app). Esto evita el
     * parpadeo "grupos → vacío → grupos" en cold start.
     */
    fun reloadAfterAuthChange() {
        cacheInvalidator?.invoke()
        if (loadInFlight) {
            // Esperar a que termine la carga actual; si es del mismo usuario, no
            // hay que hacer nada. Si fue por error (sesión distinta), reintentar.
            viewModelScope.launch {
                kotlinx.coroutines.delay(500)  // pequeño margen a que termine el load en vuelo
                if (!loadInFlight) loadGroups()
            }
            return
        }
        _uiState.update { it.copy(groups = emptyList(), participantsByGroup = emptyMap(), categoriesByGroup = emptyMap()) }
        loadGroups()
    }

    fun deleteGroup(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                groupService.deleteGroup(id)
                loadGroups()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteSpendsForGroup(
        groupId: Long,
        categoryId: Long?,
        payerId: Long?,
        beforeInstant: Instant?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.deleteSpendsFiltered(
                    groupId       = groupId,
                    categoryId    = categoryId,
                    payerId       = payerId,
                    beforeInstant = beforeInstant
                )
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun deleteSpendsByIds(ids: Set<Long>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                spendService.deleteSpendsByIds(ids.toList())
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
