package com.example.divvyup.integration.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Cantidad numérica con alineación tabular y peso destacado.
 *
 * - `FontFeatureSetting.TabularNums` activa cifras tabulares para que las
 *   cifras no salten al cambiar entre valores.
 * - Default `fontWeight = Bold` para hero numbers; el caller puede override
 *   vía [style] o parámetros de `Text`.
 *
 * @param amount valor a formatear (Double → 2 decimales, sin grouping).
 * @param currency código de moneda (3 letras) opcional; si se da se concatena.
 */
@Composable
fun AmountText(
    amount: Double,
    modifier: Modifier = Modifier,
    currency: String? = null,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val formatted = formatAmount(amount)
    val display = if (currency != null) "$formatted $currency" else formatted
    Text(
        text = display,
        modifier = modifier,
        color = color,
        style = style.copy(
            fontWeight = fontWeight,
            fontFeatureSettings = "tnum"
        )
    )
}

/**
 * Versión String de AmountText — útil cuando el caller ya tiene el texto
 * formateado y solo quiere aplicar el peso/estilo tabular.
 */
@Composable
fun AmountText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight = FontWeight.Bold
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style.copy(
            fontWeight = fontWeight,
            fontFeatureSettings = "tnum"
        )
    )
}

private fun formatAmount(amount: Double): String {
    val rounded = kotlin.math.round(amount * 100) / 100.0
    val whole = rounded.toLong()
    val cents = kotlin.math.abs(((rounded - whole) * 100).toLong())
    val sign = if (rounded < 0) "-" else ""
    return "$sign$whole.${cents.toString().padStart(2, '0')}"
}