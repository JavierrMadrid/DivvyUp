package com.example.divvyup.domain.repository

import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.User

interface GroupRepository {
    suspend fun createOrUpdateGroup(group: Group)
    suspend fun getGroups(): List<Group>
    suspend fun getGroup(groupId: String): Group?
    suspend fun addUser(groupId: String, user: User)
    suspend fun addOrUpdateSpend(groupId: String, spend: Spend)
    suspend fun removeSpend(groupId: String, spend: Spend)
    suspend fun removeUser(groupId: String, user: User)
}