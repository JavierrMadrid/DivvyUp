package com.example.divvyup.application

import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.User
import com.example.divvyup.domain.repository.GroupRepository

class GroupService(private val groupRepository: GroupRepository) {

    suspend fun createOrUpdateGroup(group: Group) {
        groupRepository.createOrUpdateGroup(group)
    }

    suspend fun getGroups(): List<Group> {
        return groupRepository.getGroups()
    }

    suspend fun getGroup(groupId: String): Group? {
        return groupRepository.getGroup(groupId)
    }

    suspend fun addUserToGroup(groupId: String, user: User) {
        groupRepository.addUser(groupId, user)
    }

    suspend fun removeUserFromGroup(groupId: String, user: User) {
        groupRepository.removeUser(groupId, user)
    }

    suspend fun addOrUpdateSpend(groupId: String, spend: Spend) {
        groupRepository.addOrUpdateSpend(groupId, spend)
    }

    suspend fun removeSpend(groupId: String, spend: Spend) {
        groupRepository.removeSpend(groupId, spend)
    }
}
