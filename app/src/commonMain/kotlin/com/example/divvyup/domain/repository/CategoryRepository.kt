package com.example.divvyup.domain.repository

import com.example.divvyup.domain.model.Category

interface CategoryRepository {
    /** Devuelve categorías globales (predefinidas) + las propias del grupo */
    suspend fun getForGroup(groupId: Long): List<Category>
    suspend fun create(category: Category): Category
    suspend fun delete(id: Long)
}

