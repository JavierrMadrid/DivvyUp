package com.example.divvyup.integration.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.divvyup.R

/**
 * Servicio Android de notificaciones locales para acciones sobre gastos.
 * Se registra una vez en MainActivity y se pasa como lambda al ViewModel.
 *
 * Canal: "divvyup_spends" - prioridad DEFAULT (sin sonido intrusivo).
 */
object SpendNotificationService {

    private const val CHANNEL_ID = "divvyup_spends"
    private const val CHANNEL_NAME = "Gastos"
    private const val CHANNEL_DESC = "Notificaciones sobre gastos añadidos, editados o eliminados"

    private var notifId = 1000

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

    fun buildNotifier(context: Context): SpendNotifier = object : SpendNotifier {
        override fun notify(event: SpendNotificationEvent) {
            val (title, body) = when (event) {
                is SpendNotificationEvent.Created ->
                    "Gasto anadido" to "\u00ab${event.concept}\u00bb · ${event.formattedAmount} ${event.currency}"
                is SpendNotificationEvent.Updated ->
                    "Gasto editado" to "\u00ab${event.concept}\u00bb actualizado · ${event.formattedAmount} ${event.currency}"
                is SpendNotificationEvent.Deleted ->
                    "Gasto eliminado" to "\u00ab${event.concept}\u00bb fue eliminado del grupo"
                is SpendNotificationEvent.BulkDeleted ->
                    "Gastos eliminados" to "${event.count} gastos eliminados del grupo"
            }

            val nm = NotificationManagerCompat.from(context)
            if (!nm.areNotificationsEnabled()) return

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
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
