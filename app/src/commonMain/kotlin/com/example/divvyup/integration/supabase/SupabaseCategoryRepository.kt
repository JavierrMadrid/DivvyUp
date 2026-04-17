package com.example.divvyup.integration.supabase

import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.repository.CategoryRepository
import com.example.divvyup.integration.supabase.dto.CategoryDto
import com.example.divvyup.integration.supabase.dto.toDomain
import com.example.divvyup.integration.supabase.dto.toDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

class SupabaseCategoryRepository(private val postgrest: Postgrest) : CategoryRepository {

    /** Devuelve categorías globales (group_id IS NULL) + las propias del grupo */
    override suspend fun getForGroup(groupId: Long): List<Category> = try {
        // Dos queries para evitar OR en RLS (skill: security-rls-performance — avoid complex RLS)
        val globals = postgrest.from("categories")
            .select { filter { filter("group_id", FilterOperator.IS, "null") } }
            .decodeList<CategoryDto>()

        val groupCategories = postgrest.from("categories")
            .select { filter { eq("group_id", groupId) } }
            .decodeList<CategoryDto>()

        (globals + groupCategories)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<CategoryDto> { it.isDefault }.thenBy { it.name })
            .map { it.toDomain() }
    } catch (e: Exception) {
        throw Exception("Error al obtener categorías: ${e.message}", e)
    }

    override suspend fun create(category: Category): Category = try {
        postgrest.from("categories")
            .insert(category.toDto()) { select() }
            .decodeSingle<CategoryDto>()
            .toDomain()
    } catch (e: Exception) {
        throw Exception("Error al crear categoría: ${e.message}", e)
    }

    override suspend fun delete(id: Long) = try {
        postgrest.from("categories")
            .delete { filter { eq("id", id) } }
        Unit
    } catch (e: Exception) {
        throw Exception("Error al eliminar categoría: ${e.message}", e)
    }
}

