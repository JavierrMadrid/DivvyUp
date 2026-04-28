package com.example.divvyup.integration.ui.screens.analytics

/** Entrada para el gráfico de dona (por categoría). */
internal data class DonutEntry(
    val label: String,
    val icon: String,
    val value: Float,
    val count: Int = 0
)

/** Entrada para los gráficos de barras (mensual / por pagador). */
internal data class BarEntry(val label: String, val value: Float)

/** Agrupación interna de gastos por categoría para analíticas. */
internal data class CategoryBucket(
    val id: Long?,
    val icon: String,
    val name: String,
    val total: Double,
    val count: Int
)

