package com.example.divvyup.integration.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Servicio Android de notificaciones locales para acciones sobre gastos.
 * Se registra una vez en MainActivity y se pasa como lambda al ViewModel.
 *
 * Canal: "divvyup_spends" — prioridad DEFAULT (sin sonido intrusivo).
 */
object SpendNotificationService {

    private const val CHANNEL_ID = "divvyup_spends"
    private const val CHANNEL_NAME = "Gastos"
    private const val CHANNEL_DESC = "Notificaciones sobre gastos añadidos, editados o eliminados"

    private var notifId = 1000

    /**
     * Registra el canal de notificaciones (llamar una vez en onCreate).
     * Requiere API 26+ (minSdk = 26 ✓).
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
            enableVibration(false)
            setSound(null, null)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * Devuelve un [SpendNotifier] listo para usar desde el ViewModel.
     * El ViewModel llama a [SpendNotifier.notify] sin conocer detalles de Android.
     */
    fun buildNotifier(context: Context): SpendNotifier = object : SpendNotifier {
        override fun notify(event: SpendNotificationEvent) {
            val (title, body) = when (event) {
                is SpendNotificationEvent.Created ->
                    "💸 Gasto añadido" to "«${event.concept}» · ${event.formattedAmount} ${event.currency}"
                is SpendNotificationEvent.Updated ->
                    "✏️ Gasto editado" to "«${event.concept}» actualizado · ${event.formattedAmount} ${event.currency}"
                is SpendNotificationEvent.Deleted ->
                    "🗑️ Gasto eliminado" to "«${event.concept}» fue eliminado del grupo"
                is SpendNotificationEvent.BulkDeleted ->
                    "🗑️ Gastos eliminados" to "${event.count} gastos eliminados del grupo"
            }

            // Verificar permiso en tiempo de ejecución (Android 13+)
            val nm = NotificationManagerCompat.from(context)
            if (!nm.areNotificationsEnabled()) return

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            try {
                nm.notify(notifId++, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS no concedido en Android 13+
            }
        }
    }
}

/** Sealed class de eventos de notificación — puro Kotlin, sin Android imports. */
sealed class SpendNotificationEvent {
    data class Created(val concept: String, val formattedAmount: String, val currency: String) : SpendNotificationEvent()
    data class Updated(val concept: String, val formattedAmount: String, val currency: String) : SpendNotificationEvent()
    data class Deleted(val concept: String) : SpendNotificationEvent()
    data class BulkDeleted(val count: Int) : SpendNotificationEvent()
}

/** Interfaz sin dependencias Android — se puede inyectar en commonMain via constructor. */
fun interface SpendNotifier {
    fun notify(event: SpendNotificationEvent)
}

