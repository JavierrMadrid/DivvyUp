package com.example.divvyup.domain.repository

import com.example.divvyup.domain.model.Participant

interface ParticipantRepository {
    suspend fun getByGroup(groupId: Long): List<Participant>
    suspend fun create(participant: Participant): Participant
    suspend fun update(participant: Participant): Participant
    suspend fun delete(id: Long)
}

