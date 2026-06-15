package com.example.divvyup.integration.notification

sealed class SpendNotificationEvent {
    abstract val groupId: Long
    data class Created(
        override val groupId: Long,
        val concept: String,
        val formattedAmount: String,
        val currency: String,
        val actorName: String? = null
    ) : SpendNotificationEvent()
    data class Updated(
        override val groupId: Long,
        val concept: String,
        val formattedAmount: String,
        val currency: String,
        val actorName: String? = null
    ) : SpendNotificationEvent()
    data class Deleted(
        override val groupId: Long,
        val concept: String,
        val actorName: String? = null
    ) : SpendNotificationEvent()
    data class BulkDeleted(override val groupId: Long, val count: Int) : SpendNotificationEvent()
    data class Activity(
        override val groupId: Long,
        val title: String,
        val body: String,
        val actorName: String? = null
    ) : SpendNotificationEvent()
}

fun interface SpendNotifier {
    fun notify(event: SpendNotificationEvent)
}

