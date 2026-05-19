package com.example.divvyup.integration.notification

sealed class SpendNotificationEvent {
    data class Created(val concept: String, val formattedAmount: String, val currency: String) : SpendNotificationEvent()
    data class Updated(val concept: String, val formattedAmount: String, val currency: String) : SpendNotificationEvent()
    data class Deleted(val concept: String) : SpendNotificationEvent()
    data class BulkDeleted(val count: Int) : SpendNotificationEvent()
}

fun interface SpendNotifier {
    fun notify(event: SpendNotificationEvent)
}

