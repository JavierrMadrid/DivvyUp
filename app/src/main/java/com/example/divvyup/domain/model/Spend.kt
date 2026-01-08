package com.example.divvyup.domain.model

import java.util.Date

data class Spend (
    val id: String = "",
    val concept: String = "",
    val amount: Double = 0.0,
    val date: Date = Date(),
    val payerId: Long = 0,
    val participantsIds: List<Long> = emptyList()
)