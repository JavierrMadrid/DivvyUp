@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.example.divvyup.application

import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.DebtTransfer
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Settlement
import com.example.divvyup.domain.model.Spend
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.round

/**
 * Genera un CSV multi-sección compatible con Excel/Google Sheets con todas las
 * secciones de analíticas: resumen, por categoría, por pagador, balances,
 * liquidaciones y lista completa de gastos.
 * Puro Kotlin — sin imports de framework (KMP commonMain).
 */
object GroupExcelExportService {

    fun buildAnalyticsExcel(
        group: Group,
        participants: List<Participant>,
        spends: List<Spend>,
        categories: List<Category>,
        balances: List<ParticipantBalance>,
        debtTransfers: List<DebtTransfer>,
        settlements: List<Settlement>,
        periodLabel: String = "Todos los períodos"
    ): String {
        val tz = TimeZone.currentSystemDefault()
        val categoryById = categories.associateBy { it.id }
        val participantById = participants.associateBy { it.id }
        val visibleSpends = spends.filterNot { it.notes.startsWith("__settlement_id:") }

        val byCategory = visibleSpends.groupBy { it.categoryId }
            .map { (catId, list) ->
                val cat = catId?.let { categoryById[it] }
                Triple(cat?.icon ?: "📦", cat?.name ?: "Sin categoría", list)
            }.sortedByDescending { it.third.sumOf { s -> s.amount } }

        val byPayer = visibleSpends.groupBy { it.payerId }
            .map { (payerId, list) ->
                Pair(participantById[payerId]?.name ?: "Desconocido", list)
            }.sortedByDescending { it.second.sumOf { s -> s.amount } }

        val totalAmount = visibleSpends.sumOf { it.amount }
        val avgAmount = if (visibleSpends.isNotEmpty()) totalAmount / visibleSpends.size else 0.0

        val pairNet = mutableMapOf<Pair<Long, Long>, Double>()
        for (s in settlements) {
            val a = s.fromParticipantId; val b = s.toParticipantId
            if (a < b) pairNet[a to b] = (pairNet[a to b] ?: 0.0) + s.amount
            else       pairNet[b to a] = (pairNet[b to a] ?: 0.0) - s.amount
        }
        val netSettlements = pairNet.mapNotNull { (pair, net) ->
            when {
                net > 0.005  -> Triple(pair.first, pair.second, net)
                net < -0.005 -> Triple(pair.second, pair.first, -net)
                else         -> null
            }
        }.sortedByDescending { it.third }

        return buildString {
            // ── PORTADA ──────────────────────────────────────────────────────
            appendLine("sep=,")
            appendLine("GRUPO,${group.name.csvEscape()}")
            appendLine("Moneda,${group.currency}")
            appendLine("Período,${periodLabel.csvEscape()}")
            if (group.description.isNotBlank()) appendLine("Descripción,${group.description.csvEscape()}")
            appendLine("Participantes,${participants.joinToString(" | ") { it.name }.csvEscape()}")
            appendLine("Total gastos,${totalAmount.fmt2()}")
            appendLine("Núm. gastos,${visibleSpends.size}")
            appendLine("Gasto promedio,${avgAmount.fmt2()}")
            appendLine()

            // ── RESUMEN POR CATEGORÍA ─────────────────────────────────────────
            appendLine("RESUMEN POR CATEGORÍA")
            appendLine("Categoría,Gastos (núm.),Total (${group.currency}),Porcentaje,Presupuesto (${group.currency}),Estado")
            byCategory.forEach { (icon, name, list) ->
                val total = list.sumOf { it.amount }
                val pct = if (totalAmount > 0) (total / totalAmount * 100) else 0.0
                val cat = categories.firstOrNull { it.name == name }
                val budget = cat?.budget
                val status = when {
                    budget == null       -> ""
                    total > budget       -> "⚠ Superado en ${(total - budget).fmt2()}"
                    total > budget * 0.8 -> "⚡ Cerca del límite"
                    else                 -> "✓ OK"
                }
                appendLine(
                    listOf(
                        "$icon $name".csvEscape(),
                        list.size.toString(),
                        total.fmt2(),
                        "${pct.fmt2()}%",
                        budget?.fmt2() ?: "",
                        status.csvEscape()
                    ).joinToString(",")
                )
            }
            appendLine()

            // ── RESUMEN POR PAGADOR ───────────────────────────────────────────
            appendLine("RESUMEN POR PAGADOR")
            appendLine("Pagador,Gastos (núm.),Total pagado (${group.currency}),Porcentaje")
            byPayer.forEach { (name, list) ->
                val total = list.sumOf { it.amount }
                val pct = if (totalAmount > 0) (total / totalAmount * 100) else 0.0
                appendLine(
                    listOf(
                        name.csvEscape(),
                        list.size.toString(),
                        total.fmt2(),
                        "${pct.fmt2()}%"
                    ).joinToString(",")
                )
            }
            appendLine()

            // ── BALANCES ──────────────────────────────────────────────────────
            appendLine("BALANCES")
            appendLine("Participante,Total pagado,Total debe,Liquidaciones recibidas,Liquidaciones enviadas,Balance neto,Estado")
            balances.sortedByDescending { it.netBalance }.forEach { b ->
                val status = when {
                    b.netBalance > 0.005  -> "Le deben ${b.netBalance.fmt2()} ${group.currency}"
                    b.netBalance < -0.005 -> "Debe ${abs(b.netBalance).fmt2()} ${group.currency}"
                    else                  -> "Al día ✓"
                }
                appendLine(
                    listOf(
                        b.participantName.csvEscape(),
                        b.totalPaid.fmt2(),
                        b.totalOwed.fmt2(),
                        b.settlementsReceived.fmt2(),
                        b.settlementsSent.fmt2(),
                        b.netBalance.fmt2(),
                        status.csvEscape()
                    ).joinToString(",")
                )
            }
            appendLine()

            // ── LIQUIDACIONES SUGERIDAS ───────────────────────────────────────
            if (debtTransfers.isNotEmpty()) {
                appendLine("LIQUIDACIONES SUGERIDAS")
                appendLine("De,A,Importe (${group.currency})")
                debtTransfers.forEach { t ->
                    appendLine(
                        listOf(
                            t.fromName.csvEscape(),
                            t.toName.csvEscape(),
                            t.amount.fmt2()
                        ).joinToString(",")
                    )
                }
                appendLine()
            }

            // ── LIQUIDACIONES NETAS (historial) ───────────────────────────────
            if (netSettlements.isNotEmpty()) {
                appendLine("LIQUIDACIONES NETAS (historial)")
                appendLine("De,A,Importe neto (${group.currency})")
                netSettlements.forEach { (fromId, toId, amount) ->
                    appendLine(
                        listOf(
                            (participantById[fromId]?.name ?: "?").csvEscape(),
                            (participantById[toId]?.name ?: "?").csvEscape(),
                            amount.fmt2()
                        ).joinToString(",")
                    )
                }
                appendLine()
            }

            // ── GASTOS DETALLADOS ─────────────────────────────────────────────
            appendLine("GASTOS DETALLADOS")
            appendLine("Fecha,Concepto,Importe (${group.currency}),Pagador,Categoría,Tipo de reparto,Notas")
            visibleSpends.sortedByDescending { it.date }.forEach { spend ->
                val dt = spend.date.toLocalDateTime(tz).date
                val dateStr = "${dt.day.toString().padStart(2, '0')}/${dt.month.number.toString().padStart(2, '0')}/${dt.year}"
                val payer = participantById[spend.payerId]?.name?.csvEscape() ?: ""
                val cat = categoryById[spend.categoryId]?.let { "${it.icon} ${it.name}" }?.csvEscape() ?: "Sin categoría"
                appendLine(
                    listOf(
                        dateStr,
                        spend.concept.csvEscape(),
                        spend.amount.fmt2(),
                        payer,
                        cat,
                        spend.splitType.name,
                        spend.notes.csvEscape()
                    ).joinToString(",")
                )
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun Double.fmt2(): String {
        val r = round(this * 100) / 100.0
        val intPart = r.toLong()
        val decPart = (abs(r % 1) * 100).toLong().toString().padStart(2, '0')
        return "$intPart.$decPart"
    }

    private fun String.csvEscape(): String {
        val needsQuotes = contains(',') || contains('"') || contains('\n') || contains('\r')
        return if (needsQuotes) "\"${replace("\"", "\"\"")}\"" else this
    }
}

