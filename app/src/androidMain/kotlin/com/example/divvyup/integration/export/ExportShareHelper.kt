package com.example.divvyup.integration.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.divvyup.application.AnalyticsExportData
import com.example.divvyup.application.GroupExcelExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Genera y comparte archivos de exportación (PDF y Excel/CSV) usando
 * FileProvider para compartir con otras apps sin permisos de almacenamiento.
 */
object ExportShareHelper {

    private const val AUTHORITY_SUFFIX = ".fileprovider"

    /**
     * Genera el PDF con [PdfGenerator], lo guarda en caché y lanza el share intent.
     * Debe llamarse desde una coroutine (IO dispatcher internamente).
     */
    suspend fun sharePdf(context: Context, data: AnalyticsExportData) {
        val bytes = withContext(Dispatchers.Default) {
            PdfGenerator.generate(data)
        }
        val safeName = data.group.name.toSafeFileName()
        val file = saveToCache(context, bytes, "divvyup_${safeName}.pdf")
        shareFile(context, file, "application/pdf", "Exportar PDF — ${data.group.name}")
    }

    /**
     * Genera el CSV analítico con [GroupExcelExportService], lo guarda y lanza el share intent.
     * Debe llamarse desde una coroutine (IO dispatcher internamente).
     */
    suspend fun shareExcel(context: Context, data: AnalyticsExportData) {
        val csv = withContext(Dispatchers.Default) {
            GroupExcelExportService.buildAnalyticsExcel(
                group        = data.group,
                participants = data.participants,
                spends       = data.spends,
                categories   = data.categories,
                balances     = data.balances,
                debtTransfers = data.debtTransfers,
                settlements  = data.settlements,
                periodLabel  = data.periodLabel
            )
        }
        val safeName = data.group.name.toSafeFileName()
        val bytes = csv.toByteArray(Charsets.UTF_8)
        // BOM UTF-8 para que Excel reconozca el encoding automáticamente
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val file = saveToCache(context, bom + bytes, "divvyup_${safeName}.csv")
        shareFile(context, file, "text/csv", "Exportar Excel — ${data.group.name}")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun saveToCache(context: Context, bytes: ByteArray, filename: String): File {
        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        return File(dir, filename).also { it.writeBytes(bytes) }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val authority = "${context.packageName}$AUTHORITY_SUFFIX"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun String.toSafeFileName(): String =
        replace(Regex("[^a-zA-Z0-9_\\-]"), "_").lowercase().take(40)
}

