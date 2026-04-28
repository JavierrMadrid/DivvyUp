package com.example.divvyup.integration.cache

import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.repository.CategoryRepository

/**
 * Decorador de caché para CategoryRepository.
 * TTL = 10 minutos — las categorías son casi estáticas (seed predefinido).
 *
 * Clave de caché: groupId (Long).
 * Invalidación: cualquier create/delete limpia la entrada del grupo afectado.
 */
class CachedCategoryRepository(
    private val delegate: CategoryRepository,
    ttlMillis: Long = 10 * 60_000L // 10 min
) : CategoryRepository {

    private val cache = InMemoryCache<Long, List<Category>>(ttlMillis)

    override suspend fun getForGroup(groupId: Long): List<Category> =
        cache.getOrLoad(groupId) { delegate.getForGroup(groupId) }

    override suspend fun create(category: Category): Category {
        val result = delegate.create(category)
        cache.invalidate(category.groupId ?: 0L)
        return result
    }

    override suspend fun update(category: Category): Category {
        val result = delegate.update(category)
        cache.invalidate(category.groupId ?: 0L)
        return result
    }

    override suspend fun delete(id: Long) {
        // Necesitamos limpiar toda la caché porque no tenemos el groupId aquí
        cache.clear()
        delegate.delete(id)
    }
}

