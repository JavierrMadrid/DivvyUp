package com.example.divvyup.integration.supabase

import com.example.divvyup.domain.model.Spend
import com.example.divvyup.domain.model.SpendShare
import com.example.divvyup.domain.repository.SpendRepository
import com.example.divvyup.integration.supabase.dto.SpendDto
import com.example.divvyup.integration.supabase.dto.SpendShareDto
import com.example.divvyup.integration.supabase.dto.toDomain
import com.example.divvyup.integration.supabase.dto.toDto
import com.example.divvyup.integration.supabase.dto.toUpdateDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseSpendRepository(private val postgrest: Postgrest) : SpendRepository {

    override suspend fun getByGroup(groupId: Long): List<Spend> = try {
        // Índice compuesto (group_id, date DESC) usado aquí (skill: query-composite-indexes)
        postgrest.from("spends")
            .select {
                filter { eq("group_id", groupId) }
                order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<SpendDto>()
            .map { it.toDomain() }
    } catch (e: Exception) {
        throw Exception("Error al obtener gastos: ${e.message}", e)
    }

    override suspend fun getSharesBySpend(spendId: Long): List<SpendShare> = try {
        postgrest.from("spend_shares")
            .select { filter { eq("spend_id", spendId) } }
            .decodeList<SpendShareDto>()
            .map { it.toDomain() }
    } catch (e: Exception) {
        throw Exception("Error al obtener el reparto del gasto: ${e.message}", e)
    }

    override suspend fun getSharesByParticipant(participantId: Long): List<SpendShare> = try {
        postgrest.from("spend_shares")
            .select { filter { eq("participant_id", participantId) } }
            .decodeList<SpendShareDto>()
            .map { it.toDomain() }
    } catch (e: Exception) {
        throw Exception("Error al obtener las participaciones del usuario: ${e.message}", e)
    }

    override suspend fun create(spend: Spend, shares: List<SpendShare>): Spend = try {
        // 1. Insertar el gasto y obtener el id generado
        val createdSpend = postgrest.from("spends")
            .insert(spend.toDto()) { select() }
            .decodeSingle<SpendDto>()
            .toDomain()

        // 2. Insertar los shares con el spendId real (data-batch-inserts: una sola llamada)
        val shareDtos = shares.map { it.toDto(resolvedSpendId = createdSpend.id) }
        postgrest.from("spend_shares").insert(shareDtos)

        createdSpend
    } catch (e: Exception) {
        throw Exception("Error al crear el gasto: ${e.message}", e)
    }

    override suspend fun update(spend: Spend, shares: List<SpendShare>): Spend = try {
        // 1. Actualizar el gasto — usamos toUpdateDto() para excluir el id (identity column)
        val updatedSpend = postgrest.from("spends")
            .update(spend.toUpdateDto()) {
                select()
                filter { eq("id", spend.id) }
            }
            .decodeSingle<SpendDto>()
            .toDomain()

        // 2. Borrar shares anteriores y reinsertar
        postgrest.from("spend_shares")
            .delete { filter { eq("spend_id", spend.id) } }

        val shareDtos = shares.map { it.toDto(resolvedSpendId = spend.id) }
        postgrest.from("spend_shares").insert(shareDtos)

        updatedSpend
    } catch (e: Exception) {
        throw Exception("Error al actualizar el gasto: ${e.message}", e)
    }

    override suspend fun delete(id: Long) = try {
        postgrest.from("spends")
            .delete { filter { eq("id", id) } }
        Unit
    } catch (e: Exception) {
        throw Exception("Error al eliminar el gasto: ${e.message}", e)
    }

    override suspend fun deleteAll(ids: List<Long>) {
        if (ids.isEmpty()) return
        try {
            postgrest.from("spends")
                .delete { filter { isIn("id", ids) } }
        } catch (e: Exception) {
            throw Exception("Error al eliminar los gastos: ${e.message}", e)
        }
    }
}

