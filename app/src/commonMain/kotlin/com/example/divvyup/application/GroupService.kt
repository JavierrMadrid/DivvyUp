package com.example.divvyup.application

import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.repository.GroupRepository

/**
 * Orquesta la lógica de negocio de grupos.
 * Recibe repositorio por constructor (DI manual, sin framework).
 * (skill: android-clean-architecture — UseCase/Service pattern)
 */
class GroupService(private val groupRepository: GroupRepository) {

    suspend fun getAllGroups(): List<Group> =
        groupRepository.getAll()

    suspend fun getGroup(id: Long): Group =
        groupRepository.getById(id)

    suspend fun createGroup(name: String, description: String = "", currency: String = "EUR"): Group {
        require(name.isNotBlank()) { "El nombre del grupo no puede estar vacío" }
        return groupRepository.create(
            Group(name = name.trim(), description = description.trim(), currency = currency)
        )
    }

    suspend fun updateGroup(group: Group): Group {
        require(group.name.isNotBlank()) { "El nombre del grupo no puede estar vacío" }
        return groupRepository.update(group.copy(name = group.name.trim()))
    }

    suspend fun deleteGroup(id: Long) =
        groupRepository.delete(id)
}

