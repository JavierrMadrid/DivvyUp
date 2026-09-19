package com.example.divvyup.application
import com.example.divvyup.domain.model.Category
import com.example.divvyup.domain.model.DebtTransfer
import com.example.divvyup.domain.model.Group
import com.example.divvyup.domain.model.Participant
import com.example.divvyup.domain.model.ParticipantBalance
import com.example.divvyup.domain.model.Spend
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.round
/**
 * Genera resumenes exportables del grupo en texto plano o CSV basico.
 * Para Excel completo ver [GroupExcelExportService].
 * Para PDF (HTML) ver [GroupPdfExportService].
 * Puro Kotlin - sin imports de framework (KMP commonMain).
 */
object GroupExportService {
    // -- Texto plano ----------------------------------------------------------
    fun buildTextSummary(
        group: Group,
        participants: List<Participant>,
        spends: List<Spend>,
        categories: List<Category>,
        balances: List<ParticipantBalance>,
        debtTransfers: List<DebtTransfer>
    ): String {
        val tz = TimeZone.currentSystemDefault()
        val categoryById = categories.associateBy { it.id }
        val participantById = participants.associateBy { it.id }
        val visibleSpends = spends.filterNot { spend ->
            spend.notes.startsWith("__settlement_id:")
        }
        val totalAmount = visibleSpends.sumOf { it.amount }.fmt2()
        return buildString {
            appendLine("=======================================")
            appendLine("  RESUMEN DE GASTOS -- ${group.name.uppercase()}")
            appendLine("=======================================")
            appendLine("Moneda: ${group.currency}")
            if (group.description.isNotBlank()) appendLine("Descripcion: ${group.description}")
            appendLine("Participantes: ${participants.joinToString(", ") { it.name }}")
            appendLine("Total de gastos: $totalAmount ${group.currency}")
            appendLine()
            appendLine("---------------------------------------")
            appendLine("GASTOS (${visibleSpends.size})")
            appendLine("---------------------------------------")
            if (visibleSpends.isEmpty()) {
                appendLine("  Sin gastos registrados.")
            } else {
                visibleSpends.sortedByDescending { it.date }.forEach { spend ->
                    val dateStr = spend.date.toLocalDateTime(tz).date.let {
                        "${it.day.toString().padStart(2, '0')}/${it.month.number.toString().padStart(2, '0')}/${it.year}"
                    }
                    val payer = participantById[spend.payerId]?.name ?: "Desconocido"
                    val cat = categoryById[spend.categoryId]?.let { "${it.icon} ${it.name}" } ?: "Sin categoria"
                    appendLine("  [$dateStr] ${spend.concept}")
                    appendLine("    Importe : ${spend.amount.fmt2()} ${group.currency}")
                    appendLine("    Pagador : $payer")
                    appendLine("    Categoria: $cat")
                    if (spend.notes.isNotBlank()) appendLine("    Notas   : ${spend.notes}")
                }
            }
            appendLine()
            appendLine("---------------------------------------")
            appendLine("BALANCES")
            appendLine("---------------------------------------")
            balances.sortedByDescending { it.netBalance }.forEach { b ->
                val sign = if (b.netBalance >= 0) "+" else ""
                val status = when {
                    b.netBalance > 0.005  -> "le deben ${b.netBalance.fmt2()} ${group.currency}"
                    b.netBalance < -0.005 -> "debe ${abs(b.netBalance).fmt2()} ${group.currency}"
                    else                  -> "esta al dia"
                }
                appendLine("  ${b.participantName.padEnd(20)} $sign${b.netBalance.fmt2()} -> $status")
            }
            appendLine()
            if (debtTransfers.isNotEmpty()) {
                appendLine("---------------------------------------")
                appendLine("LIQUIDACIONES SUGERIDAS")
                appendLine("---------------------------------------")
                debtTransfers.forEach { t ->
                    appendLine("  ${t.fromName} -> ${t.toName}  ${t.amount.fmt2()} ${group.currency}")
                }
                appendLine()
            }
            appendLine("=======================================")
            appendLine("Generado con DivvyUp")
        }
    }
    // -- CSV basico -----------------------------------------------------------
    fun buildCsv(
        group: Group,
        participants: List<Participant>,
        spends: List<Spend>,
        categories: List<Category>,
        balances: List<ParticipantBalance>
    ): String {
        val tz = TimeZone.currentSystemDefault()
        val categoryById = categories.associateBy { it.id }
        val participantById = participants.associateBy { it.id }
        val visibleSpends = spends.filterNot { it.notes.startsWith("__settlement_id:") }
        return buildString {
            appendLine("# DivvyUp -- Grupo: ${group.name.csvEscape()}")
            appendLine("# Moneda: ${group.currency}")
            appendLine()
            appendLine("GASTOS")
            appendLine("Fecha,Concepto,Importe,Moneda,Pagador,Categoria,Tipo de reparto,Notas")
            visibleSpends.sortedByDescending { it.date }.forEach { spend ->
                val dt = spend.date.toLocalDateTime(tz).date
                val dateStr = "${dt.year}-${dt.month.number.toString().padStart(2, '0')}-${dt.day.toString().padStart(2, '0')}"
                val payer = participantById[spend.payerId]?.name?.csvEscape() ?: ""
                val cat = categoryById[spend.categoryId]?.name?.csvEscape() ?: ""
                appendLine(
                    listOf(
                        dateStr,
                        spend.concept.csvEscape(),
                        spend.amount.fmt2(),
                        group.currency,
                        payer,
                        cat,
                        spend.splitType.name,
                        spend.notes.csvEscape()
                    ).joinToString(",")
                )
            }
            appendLine()
            appendLine("BALANCES")
            appendLine("Participante,Total pagado,Total debe,Liquidaciones recibidas,Liquidaciones enviadas,Balance neto,Moneda")
            balances.sortedByDescending { it.netBalance }.forEach { b ->
                appendLine(
                    listOf(
                        b.participantName.csvEscape(),
                        b.totalPaid.fmt2(),
                        b.totalOwed.fmt2(),
                        b.settlementsReceived.fmt2(),
                        b.settlementsSent.fmt2(),
                        b.netBalance.fmt2(),
                        group.currency
                    ).joinToString(",")
                )
            }
        }
    }
    // -- Helpers --------------------------------------------------------------
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