package com.example.divvyup.domain.repository

import com.example.divvyup.domain.model.Spend
import kotlin.time.Instant

/**
 * Cursor de paginación keyset para la lista de gastos.
 *
 * La lista se ordena por (date DESC, id DESC). El cursor apunta al último gasto
 * de la página ya cargada; la siguiente página continúa por (date < cursor.date)
 * o (date = cursor.date AND id < cursor.id), evitando saltos/duplicados cuando
 * se insertan gastos nuevos entre páginas.
 */
data class SpendCursor(
    val date: Instant,
    val id: Long
)

/** Página de gastos con indicador de si existen más filas posteriores. */
data class SpendPage(
    val items: List<Spend>,
    val hasMore: Boolean
)
