package com.example.divvyup.domain.model

/** Frecuencia de repetición de un gasto recurrente. */
enum class Recurrence {
    NONE,     // Gasto puntual (por defecto)
    DAILY,    // Diario
    WEEKLY,   // Semanal
    MONTHLY   // Mensual
}

