package com.example.divvyup.integration.supabase

import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.repository.GroupRepository
import com.example.divvyup.integration.supabase.dto.GroupDto
import com.example.divvyup.integration.supabase.dto.toDomain
import com.example.divvyup.integration.supabase.dto.toDto
import com.example.divvyup.integration.supabase.dto.toUpdateDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns

class SupabaseGroupRepository(private val postgrest: Postgrest) : GroupRepository {

    override suspend fun getAll(): List<Group> = try {
        postgrest.from("groups")
            .select()
            .decodeList<GroupDto>()
            .map { it.toDomain() }
    } catch (e: Exception) {
        throw Exception("Error al obtener grupos: ${e.message}", e)
    }

    override suspend fun getById(id: Long): Group = try {
        postgrest.from("groups")
            .select { filter { eq("id", id) } }
            .decodeSingle<GroupDto>()
            .toDomain()
    } catch (e: Exception) {
        throw Exception("Error al obtener el grupo: ${e.message}", e)
    }

    override suspend fun create(group: Group): Group = try {
        postgrest.from("groups")
            .insert(group.toDto()) { select() }
            .decodeSingle<GroupDto>()
            .toDomain()
    } catch (e: Exception) {
        throw Exception("Error al crear el grupo: ${e.message}", e)
    }

    override suspend fun update(group: Group): Group = try {
        postgrest.from("groups")
            .update(group.toUpdateDto()) {
                select()
                filter { eq("id", group.id) }
            }
            .decodeSingle<GroupDto>()
            .toDomain()
    } catch (e: Exception) {
        throw Exception("Error al actualizar el grupo: ${e.message}", e)
    }

    override suspend fun delete(id: Long) = try {
        postgrest.from("groups")
            .delete { filter { eq("id", id) } }
        Unit
    } catch (e: Exception) {
        throw Exception("Error al eliminar el grupo: ${e.message}", e)
    }
}

