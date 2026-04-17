package com.example.divvyup.integration.supabase

import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.repository.SettlementRepository
import com.example.divvyup.integration.supabase.dto.ParticipantBalanceDto
import com.example.divvyup.integration.supabase.dto.SettlementDto
import com.example.divvyup.integration.supabase.dto.toDomain
import com.example.divvyup.integration.supabase.dto.toDto
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseSettlementRepository(private val postgrest: Postgrest) : SettlementRepository {

    override suspend fun getByGroup(groupId: Long): List<Settlement> = try {
        postgrest.from("settlements")
            .select {
                filter { eq("group_id", groupId) }
                order("date", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }
            .decodeList<SettlementDto>()
            .map { it.toDomain() }
    } catch (e: Exception) {
        throw Exception("Error al obtener liquidaciones: ${e.message}", e)
    }

    override suspend fun create(settlement: Settlement): Settlement = try {
        postgrest.from("settlements")
            .insert(settlement.toDto()) { select() }
            .decodeSingle<SettlementDto>()
            .toDomain()
    } catch (e: Exception) {
        throw Exception("Error al registrar liquidación: ${e.message}", e)
    }

    override suspend fun delete(id: Long) = try {
        postgrest.from("settlements")
            .delete { filter { eq("id", id) } }
        Unit
    } catch (e: Exception) {
        throw Exception("Error al eliminar liquidación: ${e.message}", e)
    }

    /** Consulta la vista participant_balances filtrada por grupo (security_invoker=on respeta RLS) */
    override suspend fun getBalances(groupId: Long): List<ParticipantBalance> = try {
        postgrest.from("participant_balances")
            .select { filter { eq("group_id", groupId) } }
            .decodeList<ParticipantBalanceDto>()
            .map { it.toDomain() }
    } catch (e: Exception) {
        throw Exception("Error al calcular balances: ${e.message}", e)
    }
}

