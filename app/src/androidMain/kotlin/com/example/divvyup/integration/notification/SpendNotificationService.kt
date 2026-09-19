package com.example.divvyup.integration.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.divvyup.MainActivity
import com.example.divvyup.R

/**
 * Servicio Android de notificaciones locales para acciones sobre gastos.
 * Se registra una vez en MainActivity y se pasa como lambda al ViewModel.
 *
 * Canal: "divvyup_spends" - prioridad DEFAULT (sin sonido intrusivo).
 * Al pulsar la notificación se abre la app en la pestaña de Actividad del grupo.
 */
object SpendNotificationService {

    private const val CHANNEL_ID = "divvyup_spends"
    private const val CHANNEL_NAME = "Gastos"
    private const val CHANNEL_DESC = "Notificaciones sobre gastos añadidos, editados o eliminados"
    const val EXTRA_GROUP_ID = "extra_group_id"
    const val EXTRA_OPEN_ACTIVITY_TAB = "extra_open_activity_tab"

    private var notifId = 1000

    private fun actorSuffix(actorName: String?): String =
        actorName?.trim()?.takeIf { it.isNotBlank() }?.let { " por $it" }.orEmpty()

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
                    "Gasto \u00ab${event.concept}\u00bb añadido${actorSuffix(event.actorName)}" to
                        "${event.formattedAmount} ${event.currency}"
                is SpendNotificationEvent.Updated ->
                    "Gasto \u00ab${event.concept}\u00bb editado${actorSuffix(event.actorName)}" to
                        "${event.formattedAmount} ${event.currency}"
                is SpendNotificationEvent.Deleted ->
                    "Gasto \u00ab${event.concept}\u00bb eliminado${actorSuffix(event.actorName)}" to
                        "Fue eliminado del grupo"
                is SpendNotificationEvent.BulkDeleted ->
                    "Gastos eliminados" to "${event.count} gastos eliminados del grupo"
                is SpendNotificationEvent.Activity ->
                    event.title + actorSuffix(event.actorName) to event.body
            }

            val nm = NotificationManagerCompat.from(context)
            if (!nm.areNotificationsEnabled()) return

            // PendingIntent: abre MainActivity y navega a la pestaña Actividad del grupo
            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_GROUP_ID, event.groupId)
                putExtra(EXTRA_OPEN_ACTIVITY_TAB, true)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                event.groupId.toInt(),
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.ic_notification))
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setColor(context.resources.getColor(android.R.color.holo_green_dark, context.theme))
                .build()

            try {
                nm.notify(notifId++, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS no concedido en Android 13+
            }
        }
    }
}
