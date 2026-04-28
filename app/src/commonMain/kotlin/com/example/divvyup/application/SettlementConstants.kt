package com.example.divvyup.application

/**
 * Prefijo embebido en el campo `notes` de un [com.example.divvyup.domain.model.Spend]
 * para identificar gastos espejo generados automáticamente por una liquidación.
 *
 * Formato: "__settlement_id:<id>[|<notas opcionales>]"
 *
 * Centralizado aquí para evitar duplicidad entre [SpendService], [SettlementService]
 * y [com.example.divvyup.integration.ui.viewmodel.GroupDetailViewModel].
 */
internal const val SETTLEMENT_NOTE_PREFIX = "__settlement_id:"

