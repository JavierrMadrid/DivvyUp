package com.example.divvyup.domain.model

// Tipo de reparto de un gasto — puro Kotlin, sin imports de framework
enum class SplitType {
    EQUAL,       // Reparto equitativo entre participantes seleccionados
    PERCENTAGE,  // Reparto por porcentaje definido por participante
    CUSTOM       // Importes fijos definidos por participante
}

