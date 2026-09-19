package com.example.divvyup.integration.cache

import com.example.divvyup.domain.model.Recurrence
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SplitType
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Caché de arranque de la primera página de gastos de un grupo.
 *
 * Persistida en el almacenamiento clave-valor de la plataforma (SharedPreferences
 * en Android / NSUserDefaults en iOS) vía multiplatform-settings, de modo que al
 * abrir la app se pinta la lista de gastos de forma instantánea antes de que la
 * primera página llegue desde Supabase.
 *
 * Los modelos de dominio no llevan anotaciones de serialización (regla del
 * proyecto), por eso se usan DTOs espejo en esta capa.
 */
interface SpendStartupCache {
    fun load(groupId: Long): SpendStartupEntry?
    fun save(groupId: Long, entry: SpendStartupEntry)
    fun clear(groupId: Long)
    fun clearAll()
}

/** Primera página persistida: gastos + si existen más páginas en el servidor. */
data class SpendStartupEntry(
    val items: List<Spend>,
    val hasMore: Boolean,
    val savedAt: Instant
)

/** Tamaño máximo de la primera página persistida. */
const val SPEND_STARTUP_PAGE_SIZE = 20

class SettingsSpendStartupCache(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SpendStartupCache {

    override fun load(groupId: Long): SpendStartupEntry? {
        val raw = settings.getStringOrNull(keyFor(groupId)) ?: return null
        return try {
            json.decodeFromString<SpendStartupEntryDto>(raw).toDomain()
        } catch (_: Exception) {
            null
        }
    }

    override fun save(groupId: Long, entry: SpendStartupEntry) {
        val trimmed = entry.copy(
            items = entry.items.take(SPEND_STARTUP_PAGE_SIZE)
        )
        settings.putString(keyFor(groupId), json.encodeToString(trimmed.toEntryDto()))
    }

    override fun clear(groupId: Long) {
        settings.remove(keyFor(groupId))
    }

    override fun clearAll() {
        settings.keys.toList()
            .filter { it.startsWith(KEY_PREFIX) }
            .forEach { settings.remove(it) }
    }

    private fun keyFor(groupId: Long): String = "$KEY_PREFIX$groupId"

    private companion object {
        const val KEY_PREFIX = "spends_startup_"
    }
}

// --- DTOs espejo de Spend para serializar en la caché ---

@Serializable
internal data class SpendCacheDto(
    val id: Long,
    val groupId: Long,
    val concept: String,
    val amount: Double,
    val date: String,
    val payerId: Long,
    val categoryId: Long? = null,
    val splitType: String = "EQUAL",
    val notes: String = "",
    val createdAt: String = "",
    val recurrence: String = "NONE",
    val receiptUrl: String? = null,
    val recurrenceParentId: Long? = null,
    val recurrenceNextDue: String? = null
)

internal fun Spend.toCacheDto() = SpendCacheDto(
    id = id,
    groupId = groupId,
    concept = concept,
    amount = amount,
    date = date.toString(),
    payerId = payerId,
    categoryId = categoryId,
    splitType = splitType.name,
    notes = notes,
    createdAt = createdAt.toString(),
    recurrence = recurrence.name,
    receiptUrl = receiptUrl,
    recurrenceParentId = recurrenceParentId,
    recurrenceNextDue = recurrenceNextDue?.toString()
)

internal fun SpendCacheDto.toDomain() = Spend(
    id = id,
    groupId = groupId,
    concept = concept,
    amount = amount,
    date = if (date.isNotEmpty()) Instant.parse(date) else Instant.fromEpochMilliseconds(0),
    payerId = payerId,
    categoryId = categoryId,
    splitType = try { SplitType.valueOf(splitType) } catch (_: Exception) { SplitType.EQUAL },
    notes = notes,
    createdAt = if (createdAt.isNotEmpty()) Instant.parse(createdAt)
                else Instant.fromEpochMilliseconds(0),
    recurrence = try { Recurrence.valueOf(recurrence) } catch (_: Exception) { Recurrence.NONE },
    receiptUrl = receiptUrl,
    recurrenceParentId = recurrenceParentId,
    recurrenceNextDue = recurrenceNextDue?.takeIf { it.isNotEmpty() }?.let { Instant.parse(it) }
)

@Serializable
internal data class SpendStartupEntryDto(
    val items: List<SpendCacheDto>,
    val hasMore: Boolean,
    val savedAt: String
)

internal fun SpendStartupEntryDto.toDomain() = SpendStartupEntry(
    items = items.map { it.toDomain() },
    hasMore = hasMore,
    savedAt = if (savedAt.isNotEmpty()) Instant.parse(savedAt) else Clock.System.now()
)

internal fun SpendStartupEntry.toEntryDto(): SpendStartupEntryDto =
    SpendStartupEntryDto(
        items = items.map { it.toCacheDto() },
        hasMore = hasMore,
        savedAt = savedAt.toString()
    )
