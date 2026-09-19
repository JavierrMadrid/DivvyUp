package com.example.divvyup.application

import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.repository.CategoryRepository

class CategoryService(private val categoryRepository: CategoryRepository) {

    suspend fun getCategories(groupId: Long): List<Category> =
        categoryRepository.getForGroup(groupId)

    suspend fun createCategory(
        groupId: Long,
        name: String,
        icon: String = "📦",
        color: String = "#6366F1"
    ): Category {
        require(name.isNotBlank()) { "El nombre de la categoría no puede estar vacío" }
        return categoryRepository.create(
            Category(groupId = groupId, name = name.trim(), icon = icon, color = color)
        )
    }

    suspend fun deleteCategory(id: Long) =
        categoryRepository.delete(id)

    suspend fun updateCategoryBudget(category: Category, budget: Double?): Category =
        categoryRepository.update(category.copy(budget = budget))
}

