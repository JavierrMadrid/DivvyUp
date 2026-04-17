package com.example.divvyup.domain.repository

import com.example.divvyup.domain.model.Group

// Interfaz en domain — implementada en integration/supabase
// Todos los métodos son suspend (skill: android-clean-architecture — Repository Interfaces)
interface GroupRepository {
    suspend fun getAll(): List<Group>
    suspend fun getById(id: Long): Group
    suspend fun create(group: Group): Group
    suspend fun update(group: Group): Group
    suspend fun delete(id: Long)
}

