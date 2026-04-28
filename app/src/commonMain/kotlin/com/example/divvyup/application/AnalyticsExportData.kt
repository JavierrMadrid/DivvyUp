package com.example.divvyup.application

import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.DebtTransfer
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.model.Spend

/**
 * Agrupa todos los datos necesarios para generar exportaciones de analíticas
 * (Excel / PDF). Se crea en el ViewModel y se pasa a los handlers de plataforma.
 */
data class AnalyticsExportData(
    val group: Group,
    val participants: List<Participant>,
    val spends: List<Spend>,
    val categories: List<Category>,
    val balances: List<ParticipantBalance>,
    val debtTransfers: List<DebtTransfer>,
    val settlements: List<Settlement>,
    val periodLabel: String = "Todos los periodos"
)

