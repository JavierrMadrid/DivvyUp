package com.example.divvyup.integration.ui.screens.analytics

/** Entrada para el gráfico de dona (por categoría). */
internal data class DonutEntry(
    val label: String,
    val icon: String,
    val value: Float,
    val count: Int = 0,
    val color: String? = null  // Color hex de la categoría (si aplica)
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

internal enum class AnalyticsCardType {
    MENSUAL,
    CATEGORIA,
    PAGADOR
}

internal enum class AnalyticsExpandedTab(val title: String) {
    ROSQUILLA("Rosquilla"),
    BARRAS("Barras"),
    RANKING("Ranking")
}

internal data class AnalyticsBreakdownEntry(
    val label: String,
    val icon: String,
    val total: Double,
    val spendCount: Int,
    val color: String? = null  // Color hex (si aplica)
)

internal data class AnalyticsRankingEntry(
    val id: Long,
    val concept: String,
    val subtitle: String,
    val amount: Double
)

