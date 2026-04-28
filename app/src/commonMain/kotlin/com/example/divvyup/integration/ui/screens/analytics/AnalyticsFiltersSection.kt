package com.example.divvyup.integration.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.components.AppFilterChip
import com.example.divvyup.integration.ui.components.rememberAppFilterChipPalette
import com.example.divvyup.integration.ui.screens.MES_NOMBRES
import com.example.divvyup.integration.ui.screens.appDatePickerColors
import com.example.divvyup.integration.ui.screens.formatLocalDate
import com.example.divvyup.integration.ui.screens.localDateToMillis
import com.example.divvyup.integration.ui.screens.millisToLocalDate
import com.example.divvyup.integration.ui.theme.DarkTextBeige200
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.JungleGreen
import com.example.divvyup.integration.ui.theme.appOutlinedTextFieldColors
import com.example.divvyup.integration.ui.viewmodel.AnalyticsPeriod
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock.System

// ---------------------------------------------------------------------------
// PeriodFilterSelector
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodFilterSelector(
    period: AnalyticsPeriod,
    currentYear: Int,
    currentMonth: Month,
    onPeriodChange: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipPalette = rememberAppFilterChipPalette(selectedColor = JungleGreen)
    val availableYears = remember(currentYear) { (currentYear - 2..currentYear).toList().reversed() }
    val periodControlBorderColor =
        if (isSystemInDarkTheme()) DarkTextBeige200 else MaterialTheme.colorScheme.outline

    var showDatePickerDesde by remember { mutableStateOf(false) }
    var showDatePickerHasta by remember { mutableStateOf(false) }

    val rangeDesde = (period as? AnalyticsPeriod.PorRango)?.desde
    val rangeHasta = (period as? AnalyticsPeriod.PorRango)?.hasta

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Período",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                PeriodChip(
                    label = "Todo",
                    isSelected = period is AnalyticsPeriod.Todo,
                    selectedColor = chipPalette.selectedColor,
                    unselectedColor = chipPalette.unselectedColor,
                    unselectedTextColor = chipPalette.unselectedTextColor,
                    onClick = { onPeriodChange(AnalyticsPeriod.Todo) }
                )
            }
            item {
                val isSelected = period is AnalyticsPeriod.PorMes
                PeriodChip(
                    label = "Por mes",
                    isSelected = isSelected,
                    selectedColor = chipPalette.selectedColor,
                    unselectedColor = chipPalette.unselectedColor,
                    unselectedTextColor = chipPalette.unselectedTextColor,
                    onClick = {
                        if (!isSelected) onPeriodChange(AnalyticsPeriod.PorMes(currentMonth, currentYear))
                    }
                )
            }
            item {
                val isSelected = period is AnalyticsPeriod.PorAnyo
                PeriodChip(
                    label = "Por año",
                    isSelected = isSelected,
                    selectedColor = chipPalette.selectedColor,
                    unselectedColor = chipPalette.unselectedColor,
                    unselectedTextColor = chipPalette.unselectedTextColor,
                    onClick = {
                        if (!isSelected) onPeriodChange(AnalyticsPeriod.PorAnyo(currentYear))
                    }
                )
            }
            item {
                val isSelected = period is AnalyticsPeriod.PorRango
                PeriodChip(
                    label = "Rango",
                    isSelected = isSelected,
                    selectedColor = chipPalette.selectedColor,
                    unselectedColor = chipPalette.unselectedColor,
                    unselectedTextColor = chipPalette.unselectedTextColor,
                    onClick = {
                        if (!isSelected) {
                            val today = System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                            onPeriodChange(AnalyticsPeriod.PorRango(desde = today, hasta = today))
                        }
                    }
                )
            }
        }

        when (period) {
            is AnalyticsPeriod.PorMes -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodDropdown(
                        options = Month.entries.map { it.number to MES_NOMBRES[it.number - 1] },
                        selected = period.month.number,
                        onSelect = { num ->
                            onPeriodChange(
                                AnalyticsPeriod.PorMes(Month.entries.first { it.number == num }, period.year)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    PeriodDropdown(
                        options = availableYears.map { it to it.toString() },
                        selected = period.year,
                        onSelect = { onPeriodChange(AnalyticsPeriod.PorMes(period.month, it)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            is AnalyticsPeriod.PorAnyo -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PeriodDropdown(
                        options = availableYears.map { it to it.toString() },
                        selected = period.year,
                        onSelect = { onPeriodChange(AnalyticsPeriod.PorAnyo(it)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            is AnalyticsPeriod.PorRango -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDatePickerDesde = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, periodControlBorderColor)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = rangeDesde?.let(::formatLocalDate) ?: "Desde", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { showDatePickerHasta = true },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, periodControlBorderColor)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(text = rangeHasta?.let(::formatLocalDate) ?: "Hasta", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            AnalyticsPeriod.Todo -> Unit
        }
    }

    if (showDatePickerDesde) {
        val state = rememberDatePickerState(initialSelectedDateMillis = rangeDesde?.let(::localDateToMillis))
        DatePickerDialog(
            onDismissRequest = { showDatePickerDesde = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val nuevaDesde = millisToLocalDate(millis)
                        val hasta = rangeHasta ?: nuevaDesde
                        onPeriodChange(
                            AnalyticsPeriod.PorRango(
                                desde = nuevaDesde,
                                hasta = if (hasta < nuevaDesde) nuevaDesde else hasta
                            )
                        )
                    }
                    showDatePickerDesde = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerDesde = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state, colors = appDatePickerColors()) }
    }

    if (showDatePickerHasta) {
        val state = rememberDatePickerState(initialSelectedDateMillis = rangeHasta?.let(::localDateToMillis))
        DatePickerDialog(
            onDismissRequest = { showDatePickerHasta = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val nuevaHasta = millisToLocalDate(millis)
                        val desde = rangeDesde ?: nuevaHasta
                        onPeriodChange(
                            AnalyticsPeriod.PorRango(
                                desde = if (desde > nuevaHasta) nuevaHasta else desde,
                                hasta = nuevaHasta
                            )
                        )
                    }
                    showDatePickerHasta = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerHasta = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state, colors = appDatePickerColors()) }
    }
}

// ---------------------------------------------------------------------------
// PeriodChip
// ---------------------------------------------------------------------------

@Composable
internal fun PeriodChip(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
    unselectedTextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppFilterChip(
        label = label,
        selected = isSelected,
        selectedColor = selectedColor,
        unselectedColor = unselectedColor,
        unselectedTextColor = unselectedTextColor,
        height = DivvyUpTokens.ChipHeight,
        onClick = onClick,
        modifier = modifier
    )
}

// ---------------------------------------------------------------------------
// PeriodDropdown
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeriodDropdown(
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second.orEmpty()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = appOutlinedTextFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

