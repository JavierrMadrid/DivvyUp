package com.example.divvyup.domain.model

/**
 * Balance neto de un participante dentro de su grupo.
 * netBalance > 0 → le deben dinero
 * netBalance < 0 → debe dinero
 */
data class ParticipantBalance(
    val participantId: Long,
    val groupId: Long,
    val participantName: String,
    val totalPaid: Double,
    val totalOwed: Double,
    val settlementsReceived: Double,
    val settlementsSent: Double,
    val netBalance: Double
)

/**
 * Transferencia simplificada: fromName debe [amount] a toName.
 * Resultado del algoritmo greedy de minimización de transferencias.
 */
data class DebtTransfer(
    val fromParticipantId: Long,
    val fromName: String,
    val toParticipantId: Long,
    val toName: String,
    val amount: Double
)

