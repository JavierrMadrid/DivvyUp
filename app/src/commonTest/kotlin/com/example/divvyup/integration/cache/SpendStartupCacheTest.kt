package com.example.divvyup.integration.cache

import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SplitType
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SpendStartupCacheTest {

    private fun spend(id: Long) = Spend(
        id = id, groupId = 7, concept = "Gasto $id", amount = 10.0,
        payerId = 1, splitType = SplitType.EQUAL,
        date = Instant.parse("2026-04-01T12:00:00Z")
    )

    @Test
    fun save_load_roundTrip_conserva_datos() {
        val cache = SettingsSpendStartupCache(MapSettings())
        val entry = SpendStartupEntry(
            items = listOf(spend(1), spend(2)),
            hasMore = true,
            savedAt = Instant.parse("2026-04-01T12:00:00Z")
        )

        cache.save(7, entry)
        val loaded = cache.load(7)

        assertTrue(loaded != null, "La entrada guardada debe poder leerse")
        assertEquals(listOf(1L, 2L), loaded!!.items.map { it.id })
        assertEquals(true, loaded.hasMore)
        assertEquals(Instant.parse("2026-04-01T12:00:00Z"), loaded.savedAt)
        assertEquals("Gasto 1", loaded.items[0].concept)
        assertEquals(SplitType.EQUAL, loaded.items[0].splitType)
    }

    @Test
    fun load_devuelve_null_si_no_hay_datos() {
        val cache = SettingsSpendStartupCache(MapSettings())
        assertNull(cache.load(7))
    }

    @Test
    fun save_guarda_por_grupo_sin_colision() {
        val cache = SettingsSpendStartupCache(MapSettings())
        cache.save(1, SpendStartupEntry(listOf(spend(1)), hasMore = false, savedAt = Instant.parse("2026-04-01T12:00:00Z")))
        cache.save(2, SpendStartupEntry(listOf(spend(2)), hasMore = true, savedAt = Instant.parse("2026-04-01T12:00:00Z")))

        assertEquals(listOf(1L), cache.load(1)!!.items.map { it.id })
        assertEquals(listOf(2L), cache.load(2)!!.items.map { it.id })
    }

    @Test
    fun save_trim_a_SPEND_STARTUP_PAGE_SIZE() {
        val cache = SettingsSpendStartupCache(MapSettings())
        val many = (1..50).map { spend(it.toLong()) }
        cache.save(7, SpendStartupEntry(many, hasMore = true, savedAt = Instant.parse("2026-04-01T12:00:00Z")))

        assertEquals(SPEND_STARTUP_PAGE_SIZE, cache.load(7)!!.items.size)
    }

    @Test
    fun clear_elimina_solo_un_grupo() {
        val cache = SettingsSpendStartupCache(MapSettings())
        cache.save(1, SpendStartupEntry(listOf(spend(1)), hasMore = false, savedAt = Instant.parse("2026-04-01T12:00:00Z")))
        cache.save(2, SpendStartupEntry(listOf(spend(2)), hasMore = false, savedAt = Instant.parse("2026-04-01T12:00:00Z")))

        cache.clear(1)

        assertNull(cache.load(1))
        assertEquals(listOf(2L), cache.load(2)!!.items.map { it.id })
    }

    @Test
    fun clearAll_elimina_todos_los_grupos() {
        val cache = SettingsSpendStartupCache(MapSettings())
        cache.save(1, SpendStartupEntry(listOf(spend(1)), hasMore = false, savedAt = Instant.parse("2026-04-01T12:00:00Z")))
        cache.save(2, SpendStartupEntry(listOf(spend(2)), hasMore = false, savedAt = Instant.parse("2026-04-01T12:00:00Z")))

        cache.clearAll()

        assertNull(cache.load(1))
        assertNull(cache.load(2))
    }

    @Test
    fun load_tolerante_a_json_corrupto() {
        val settings = MapSettings()
        settings.putString("spends_startup_7", "{no es json}")
        val cache = SettingsSpendStartupCache(settings)

        assertNull(cache.load(7))
    }
}
