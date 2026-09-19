@file:OptIn(kotlin.time.ExperimentalTime::class)
package com.example.divvyup.integration.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.divvyup.application.AnalyticsExportData
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.round

/**
 * Genera un PDF a partir de [AnalyticsExportData] usando [PdfDocument] nativo de Android.
 * No requiere dependencias externas ni permisos de almacenamiento.
 */
@Suppress("MagicNumber")
object PdfGenerator {

    private const val PAGE_WIDTH  = 595   // A4 a 72 dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN      = 32f
    private const val CONTENT_W   = PAGE_WIDTH - MARGIN * 2

    // Colores de la paleta DivvyUp
    private val GREEN_DARK   = Color.rgb(0x2d, 0x6a, 0x4f)
    private val GREEN_MID    = Color.rgb(0x40, 0x91, 0x6c)
    private val GREEN_LIGHT  = Color.rgb(0xd1, 0xfa, 0xe5)
    private val GREY_BG      = Color.rgb(0xf9, 0xfa, 0xfb)
    private val GREY_BORDER  = Color.rgb(0xe5, 0xe7, 0xeb)
    private val RED_LIGHT    = Color.rgb(0xfe, 0xe2, 0xe2)
    private val RED_TEXT     = Color.rgb(0x99, 0x1b, 0x1b)
    private val TEXT_DARK    = Color.rgb(0x11, 0x18, 0x27)
    private val TEXT_MUTED   = Color.rgb(0x6b, 0x72, 0x80)

    fun generate(data: AnalyticsExportData): ByteArray {
        val doc = PdfDocument()
        val state = PdfState(doc, data)

        state.drawHeaderPage()
        state.drawCategoryDonutChart()
        state.drawMonthlyBarChart()
        state.drawPayerBarChart()
        state.drawCategorySection()
        state.drawPayerSection()
        state.drawBalancesSection()
        state.drawSuggestedSettlementsSection()
        state.drawSpendListSection()
        state.finishPage()

        val out = java.io.ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private class PdfState(
        private val doc: PdfDocument,
        private val data: AnalyticsExportData
    ) {
        private var pageNum = 0
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = MARGIN + 8f

        private val tz = TimeZone.currentSystemDefault()
        private val catById = data.categories.associateBy { it.id }
        private val partById = data.participants.associateBy { it.id }
        private val visibleSpends = data.spends.filterNot { it.notes.startsWith("__settlement_id:") }
        private val totalAmount: Double = visibleSpends.fold(0.0) { acc, s -> acc + s.amount }

        // Paints reutilizables
        private val pSec    = paint(10f, GREEN_DARK, bold = true)
        private val pBody   = paint(9f, TEXT_DARK)
        private val pBodyB  = paint(9f, TEXT_DARK, bold = true)
        private val pMuted  = paint(8f, TEXT_MUTED)
        private val pKpiVal = paint(16f, GREEN_DARK, bold = true)
        private val pKpiLbl = paint(8f, TEXT_MUTED)

        private fun paint(size: Float, color: Int, bold: Boolean = false) = Paint().apply {
            textSize = size
            this.color = color
            if (bold) typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        // ── Gestión de páginas ────────────────────────────────────────────────

        fun newPage() {
            if (page != null) doc.finishPage(page)
            pageNum++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            page = doc.startPage(info)
            canvas = page!!.canvas
            y = MARGIN + 8f
        }

        fun finishPage() {
            if (page != null) { doc.finishPage(page); page = null }
        }

        private fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN) newPage()
        }

        private val c get() = canvas!!

        // ── Primitivas de dibujo ──────────────────────────────────────────────

        private fun drawRect(left: Float, top: Float, right: Float, bottom: Float, color: Int, radius: Float = 0f) {
            val p = Paint().apply { this.color = color; isAntiAlias = true }
            if (radius > 0f) c.drawRoundRect(RectF(left, top, right, bottom), radius, radius, p)
            else c.drawRect(left, top, right, bottom, p)
        }

        private fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, color: Int = GREY_BORDER) {
            val p = Paint().apply { this.color = color; strokeWidth = 0.5f }
            c.drawLine(x1, y1, x2, y2, p)
        }

        private fun drawText(text: String, x: Float, yPos: Float, paint: Paint, maxWidth: Float = Float.MAX_VALUE): Float {
            val truncated = if (paint.measureText(text) > maxWidth) {
                var t = text
                while (t.isNotEmpty() && paint.measureText("$t…") > maxWidth) t = t.dropLast(1)
                "$t…"
            } else text
            c.drawText(truncated, x, yPos, paint)
            return yPos
        }

        private fun drawTextRight(text: String, rightX: Float, yPos: Float, paint: Paint) {
            val w = paint.measureText(text)
            c.drawText(text, rightX - w, yPos, paint)
        }

        // ── Secciones ─────────────────────────────────────────────────────────

        fun drawHeaderPage() {
            newPage()

            // Cabecera verde
            drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 100f, GREEN_DARK)
            drawText("DivvyUp", MARGIN, 38f, paint(26f, Color.WHITE, bold = true))
            drawText(data.group.name, MARGIN, 60f, paint(14f, Color.WHITE))
            drawText("Periodo: ${data.periodLabel}", MARGIN, 78f, paint(9f, Color.WHITE).apply { alpha = 180 })
            drawText("Participantes: ${data.participants.joinToString(", ") { it.name }}", MARGIN, 92f, paint(8f, Color.WHITE).apply { alpha = 160 }, CONTENT_W)
            y = 116f

            // KPIs
            val avgAmount = if (visibleSpends.isNotEmpty()) totalAmount / visibleSpends.size else 0.0
            val kpis = listOf(
                "Total gastado" to "${totalAmount.fmt2()} ${data.group.currency}",
                "Num. gastos"   to visibleSpends.size.toString(),
                "Promedio"      to "${avgAmount.fmt2()} ${data.group.currency}",
                "Participantes" to data.participants.size.toString()
            )
            val kpiW = (CONTENT_W - 12f * 3) / 4f
            kpis.forEachIndexed { i, (label, value) ->
                val kx = MARGIN + i * (kpiW + 12f)
                drawRect(kx, y, kx + kpiW, y + 44f, GREY_BG, 6f)
                drawText(label.uppercase(), kx + 8f, y + 14f, pKpiLbl)
                drawText(value, kx + 8f, y + 34f, pKpiVal, kpiW - 10f)
            }
            y += 56f
        }

        // Cabecera de sección verde
        private fun sectionHeader(title: String) {
            ensureSpace(30f)
            drawRect(MARGIN, y, MARGIN + CONTENT_W, y + 22f, GREEN_LIGHT, 4f)
            drawText(title, MARGIN + 8f, y + 15f, pSec)
            y += 28f
        }

        // Fila de tabla con fondo alternado
        private fun tableRow(cols: List<Pair<String, Float>>, rowIndex: Int, heights: Float = 16f) {
            ensureSpace(heights + 4f)
            if (rowIndex % 2 == 0) drawRect(MARGIN, y - 2f, MARGIN + CONTENT_W, y + heights, GREY_BG)
            var x = MARGIN + 4f
            cols.forEach { (text, width) ->
                drawText(text, x, y + heights - 4f, pBody, width - 4f)
                x += width
            }
            y += heights + 2f
        }

        // Cabecera de tabla
        private fun tableHeader(cols: List<Pair<String, Float>>) {
            ensureSpace(18f)
            drawRect(MARGIN, y, MARGIN + CONTENT_W, y + 18f, GREEN_DARK)
            var x = MARGIN + 4f
            cols.forEach { (text, width) ->
                drawText(text.uppercase(), x, y + 13f, paint(8f, Color.WHITE, bold = true))
                x += width
            }
            y += 20f
        }

        fun drawCategoryDonutChart() {
            if (visibleSpends.isEmpty()) return
            val byCategory = visibleSpends.groupBy { it.categoryId }
                .map { (catId, list) ->
                    val name = catId?.let { catById[it] }?.name ?: "Sin categoría"
                    name to list.sumOf { it.amount }
                }.sortedByDescending { it.second }
            if (byCategory.isEmpty()) return

            sectionHeader("Distribución por categoría")
            ensureSpace(200f)

            val chartColors = intArrayOf(GREEN_MID, GREEN_DARK, Color.rgb(0xf5, 0x9e, 0x0b),
                Color.rgb(0x6b, 0x21, 0xa8), Color.rgb(0x0e, 0x7a, 0x9e),
                Color.rgb(0xdc, 0x26, 0x26), Color.rgb(0x16, 0xa3, 0x4a))
            val total = byCategory.sumOf { it.second }.coerceAtLeast(0.001)

            // Donut usando drawArc
            val cx = MARGIN + 100f
            val cy = y + 90f
            val outerR = 75f
            val innerR = 45f
            val strokeW = outerR - innerR
            val paintDonut = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = strokeW }

            var startAngle = -90f
            byCategory.forEachIndexed { i, (_, value) ->
                val sweep = ((value / total) * 360f).toFloat()
                paintDonut.color = chartColors[i % chartColors.size]
                c.drawArc(
                    android.graphics.RectF(cx - outerR + strokeW / 2, cy - outerR + strokeW / 2,
                        cx + outerR - strokeW / 2, cy + outerR - strokeW / 2),
                    startAngle, sweep - 1f, false, paintDonut
                )
                startAngle += sweep
            }
            // Etiqueta central
            val pCenter = paint(8f, TEXT_DARK, bold = true)
            val pCenterSmall = paint(7f, TEXT_MUTED)
            drawText(byCategory.size.toString(), cx - pCenter.measureText(byCategory.size.toString()) / 2, cy + 4f, pCenter)
            val catLabel = "categorías"
            drawText(catLabel, cx - pCenterSmall.measureText(catLabel) / 2, cy + 14f, pCenterSmall)

            // Leyenda a la derecha del donut
            val legendX = MARGIN + 200f
            var legendY = y + 6f
            byCategory.take(7).forEachIndexed { i, (name, value) ->
                val pct = (value / total * 100).toFloat()
                val dotPaint = Paint().apply { color = chartColors[i % chartColors.size]; isAntiAlias = true }
                c.drawCircle(legendX, legendY + 5f, 5f, dotPaint)
                drawText(name, legendX + 12f, legendY + 9f, pBody, CONTENT_W - 200f - 12f - 50f)
                drawTextRight("${pct.toDouble().fmt2()}%", MARGIN + CONTENT_W, legendY + 9f, pMuted)
                legendY += 18f
            }
            y = cy + outerR + 16f
        }

        fun drawMonthlyBarChart() {
            val monthly = visibleSpends.groupBy { spend ->
                val ldt = spend.date.toLocalDateTime(tz)
                ldt.year * 100 + ldt.month.number
            }.entries.sortedBy { it.key }.takeLast(12)
                .map { (key, list) ->
                    val m = key % 100
                    val yr = (key / 100).toString().takeLast(2)
                    val monthNames = arrayOf("", "Ene", "Feb", "Mar", "Abr", "May", "Jun",
                        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                    "${monthNames[m]}/$yr" to list.sumOf { it.amount }
                }
            if (monthly.size < 2) return

            sectionHeader("Evolución mensual")
            ensureSpace(140f)

            val maxVal = monthly.maxOf { it.second }.coerceAtLeast(0.001)
            val barAreaH = 100f
            val barAreaBottom = y + barAreaH
            val barW = (CONTENT_W - 4f) / monthly.size - 4f
            val barPaint = Paint().apply { color = GREEN_MID; isAntiAlias = true }
            val barPaintHighlight = Paint().apply { color = GREEN_DARK; isAntiAlias = true }

            monthly.forEachIndexed { i, (label, value) ->
                val fraction = (value / maxVal).toFloat().coerceIn(0.02f, 1f)
                val bx = MARGIN + i * (barW + 4f)
                val barH = barAreaH * fraction
                val isLast = i == monthly.size - 1
                val p = if (isLast) barPaintHighlight else barPaint
                c.drawRoundRect(
                    android.graphics.RectF(bx, barAreaBottom - barH, bx + barW, barAreaBottom),
                    3f, 3f, p
                )
                // Etiqueta de valor encima (solo si hay espacio)
                if (fraction > 0.25f) {
                    drawText(value.fmt2(), bx + 2f, barAreaBottom - barH - 2f, pMuted, barW)
                }
                // Etiqueta de mes debajo
                drawText(label, bx, barAreaBottom + 12f, pMuted, barW)
            }
            // A1 — Tendencia: flecha y %
            val lastVal = monthly.last().second
            val prevVal = monthly[monthly.size - 2].second
            if (prevVal > 0.0) {
                val trend = ((lastVal - prevVal) / prevVal * 100)
                val isUp = trend >= 0
                val trendColor = if (isUp) Color.rgb(0xdc, 0x26, 0x26) else Color.rgb(0x16, 0xa3, 0x4a)
                val arrow = if (isUp) "▲" else "▼"
                val trendStr = "$arrow ${"%.1f".format(kotlin.math.abs(trend))}% vs. mes anterior"
                drawText(trendStr, MARGIN + CONTENT_W - 150f, y + 10f, paint(8f, trendColor, bold = true))
            }
            y = barAreaBottom + 22f
        }

        fun drawPayerBarChart() {
            val byPayer = visibleSpends.groupBy { it.payerId }
                .map { (payerId, list) ->
                    (partById[payerId]?.name ?: "Desconocido") to list.sumOf { it.amount }
                }.sortedByDescending { it.second }
            if (byPayer.size < 2) return

            sectionHeader("Gasto por pagador")
            val total = byPayer.sumOf { it.second }.coerceAtLeast(0.001)
            val maxVal = byPayer.first().second.coerceAtLeast(0.001)
            val chartColors = intArrayOf(GREEN_MID, GREEN_DARK, Color.rgb(0xf5, 0x9e, 0x0b),
                Color.rgb(0x6b, 0x21, 0xa8), Color.rgb(0x0e, 0x7a, 0x9e))

            byPayer.forEachIndexed { i, (name, value) ->
                ensureSpace(26f)
                val fraction = (value / maxVal).toFloat().coerceIn(0.01f, 1f)
                val barMaxW = CONTENT_W - 130f
                val barH = 10f
                val barColor = chartColors[i % chartColors.size]
                val barY = y + 4f

                drawText(name, MARGIN, barY + 10f, pBody, 120f)
                val barX = MARGIN + 130f
                // Track
                drawRect(barX, barY, barX + barMaxW, barY + barH, GREY_BG, 4f)
                // Fill
                drawRect(barX, barY, barX + barMaxW * fraction, barY + barH, barColor, 4f)
                // Valor a la derecha
                val pct = value / total * 100
                drawTextRight("${value.fmt2()} (${pct.fmt2()}%)", MARGIN + CONTENT_W, barY + 10f, pMuted)
                y += 22f
            }
            y += 8f
        }

        fun drawCategorySection() {
            sectionHeader("Gastos por categoria")

            data class CatRow(val name: String, val total: Double, val count: Int)
            val byCategory: List<CatRow> = visibleSpends.groupBy { it.categoryId }
                .map { (catId, list) ->
                    val cat = catId?.let { catById[it] }
                    CatRow(cat?.name ?: "Sin categoria", list.fold(0.0) { acc, s -> acc + s.amount }, list.size)
                }.sortedByDescending { it.total }

            val cols = listOf(
                "Categoria" to 160f, "Gastos" to 50f, "Total" to 90f,
                "%" to 60f, "Presupuesto" to (CONTENT_W - 360f)
            )
            tableHeader(cols)
            byCategory.forEachIndexed { i, row ->
                val pct = if (totalAmount > 0) (row.total / totalAmount * 100) else 0.0
                val cat = data.categories.firstOrNull { it.name == row.name }
                val budget = cat?.budget
                val budgetStr = if (budget != null) "${budget.fmt2()} ${data.group.currency}" else "—"
                tableRow(listOf(
                    row.name to 160f,
                    row.count.toString() to 50f,
                    "${row.total.fmt2()} ${data.group.currency}" to 90f,
                    "${pct.fmt2()}%" to 60f,
                    budgetStr to (CONTENT_W - 360f)
                ), i)

                // Mini barra de progreso si hay presupuesto
                if (budget != null && budget > 0) {
                    val fraction = (row.total / budget).toFloat().coerceIn(0f, 1f)
                    val barX = MARGIN + 160f + 50f + 90f + 60f + 4f
                    val barW = CONTENT_W - 160f - 50f - 90f - 60f - 12f
                    drawRect(barX, y - 10f, barX + barW, y - 5f, GREY_BORDER, 2f)
                    val barColor = if (row.total > budget) Color.rgb(0xdc, 0x26, 0x26) else GREEN_MID
                    drawRect(barX, y - 10f, barX + barW * fraction, y - 5f, barColor, 2f)
                }
            }
            drawLine(MARGIN, y, MARGIN + CONTENT_W, y)
            y += 12f
        }

        fun drawPayerSection() {
            sectionHeader("Gastos por pagador")

            data class PayerRow(val name: String, val total: Double, val count: Int)
            val byPayer: List<PayerRow> = visibleSpends.groupBy { it.payerId }
                .map { (payerId, list) ->
                    PayerRow(partById[payerId]?.name ?: "Desconocido", list.fold(0.0) { acc, s -> acc + s.amount }, list.size)
                }.sortedByDescending { it.total }

            val cols = listOf(
                "Pagador" to 200f, "Gastos" to 50f,
                "Total" to 100f, "%" to (CONTENT_W - 350f)
            )
            tableHeader(cols)
            byPayer.forEachIndexed { i, row ->
                val pct = if (totalAmount > 0) (row.total / totalAmount * 100) else 0.0
                tableRow(listOf(
                    row.name to 200f,
                    row.count.toString() to 50f,
                    "${row.total.fmt2()} ${data.group.currency}" to 100f,
                    "${pct.fmt2()}%" to (CONTENT_W - 350f)
                ), i)
            }
            drawLine(MARGIN, y, MARGIN + CONTENT_W, y); y += 12f
        }

        fun drawBalancesSection() {
            sectionHeader("Balances")
            val cols = listOf(
                "Participante" to 150f, "Pagado" to 90f, "Debe" to 90f,
                "Balance neto" to 100f, "Estado" to (CONTENT_W - 430f)
            )
            tableHeader(cols)
            data.balances.sortedByDescending { it.netBalance }.forEachIndexed { i, b ->
                ensureSpace(18f)
                if (i % 2 == 0) drawRect(MARGIN, y - 2f, MARGIN + CONTENT_W, y + 16f, GREY_BG)
                drawText(b.participantName, MARGIN + 4f, y + 12f, pBody, 146f)
                drawText("${b.totalPaid.fmt2()} ${data.group.currency}", MARGIN + 154f, y + 12f, pBody, 86f)
                drawText("${b.totalOwed.fmt2()} ${data.group.currency}", MARGIN + 244f, y + 12f, pBody, 86f)
                val sign = if (b.netBalance >= 0) "+" else ""
                val netPaint = when {
                    b.netBalance > 0.005  -> paint(9f, GREEN_DARK, bold = true)
                    b.netBalance < -0.005 -> paint(9f, RED_TEXT, bold = true)
                    else                  -> pBodyB
                }
                drawText("$sign${b.netBalance.fmt2()}", MARGIN + 334f, y + 12f, netPaint, 96f)
                val status = when {
                    b.netBalance > 0.005  -> "Le deben"
                    b.netBalance < -0.005 -> "Debe"
                    else                  -> "Al dia"
                }
                val badgeColor = when {
                    b.netBalance > 0.005  -> GREEN_LIGHT
                    b.netBalance < -0.005 -> RED_LIGHT
                    else                  -> GREY_BORDER
                }
                val badgeTxtColor = if (b.netBalance < -0.005) RED_TEXT else GREEN_DARK
                val badgeX = MARGIN + 434f
                drawRect(badgeX, y, badgeX + 60f, y + 14f, badgeColor, 7f)
                drawText(status, badgeX + 6f, y + 11f, paint(8f, badgeTxtColor), 54f)
                y += 18f
            }
            drawLine(MARGIN, y, MARGIN + CONTENT_W, y); y += 12f
        }

        fun drawSuggestedSettlementsSection() {
            if (data.debtTransfers.isEmpty()) return
            sectionHeader("Liquidaciones sugeridas")
            val cols = listOf("De" to 180f, "A" to 180f, "Importe" to (CONTENT_W - 360f))
            tableHeader(cols)
            data.debtTransfers.forEachIndexed { i, t ->
                tableRow(listOf(
                    t.fromName to 180f,
                    t.toName to 180f,
                    "${t.amount.fmt2()} ${data.group.currency}" to (CONTENT_W - 360f)
                ), i)
            }
            drawLine(MARGIN, y, MARGIN + CONTENT_W, y); y += 12f
        }

        fun drawSpendListSection() {
            sectionHeader("Lista de gastos (${visibleSpends.size})")
            val cols = listOf(
                "Fecha" to 55f, "Concepto" to 160f, "Pagador" to 100f,
                "Categoria" to 100f, "Importe" to (CONTENT_W - 415f)
            )
            tableHeader(cols)
            visibleSpends.sortedByDescending { it.date }.forEachIndexed { i, spend ->
                val dt = spend.date.toLocalDateTime(tz).date
                val dateStr = "${dt.day.toString().padStart(2,'0')}/${dt.month.number.toString().padStart(2,'0')}/${dt.year}"
                val payer = partById[spend.payerId]?.name ?: "?"
                val cat = catById[spend.categoryId]?.name ?: "Sin cat."
                tableRow(listOf(
                    dateStr to 55f,
                    spend.concept to 160f,
                    payer to 100f,
                    cat to 100f,
                    "${spend.amount.fmt2()} ${data.group.currency}" to (CONTENT_W - 415f)
                ), i)
            }
            drawLine(MARGIN, y, MARGIN + CONTENT_W, y); y += 12f

            // Pie de página
            ensureSpace(20f)
            drawText("Generado con DivvyUp", MARGIN, y + 12f, pMuted)
            drawTextRight(data.group.name, MARGIN + CONTENT_W, y + 12f, pMuted)
            y += 16f
        }

        private fun Double.fmt2(): String {
            val r = round(this * 100) / 100.0
            val intPart = r.toLong()
            val decPart = (abs(r % 1) * 100).toLong().toString().padStart(2, '0')
            return "$intPart.$decPart"
        }
    }
}

