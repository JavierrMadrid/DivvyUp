package com.example.divvyup.integration.ui
import kotlin.test.Test
import kotlin.test.assertEquals
class DefaultSplitDefaultsTest {
    @Test
    fun resolveDefaultSplitPercentages_reparte_equitativamente_si_no_hay_configuracion() {
        val result = resolveDefaultSplitPercentages(
            participantIds = listOf(1L, 2L, 3L),
            savedPercentages = emptyMap()
        )
        assertEquals(33.333333333333336, result.getValue(1L))
        assertEquals(33.333333333333336, result.getValue(2L))
        assertEquals(33.333333333333336, result.getValue(3L))
    }
    @Test
    fun resolveDefaultSplitPercentages_normaliza_si_falta_un_participante_antiguo() {
        val result = resolveDefaultSplitPercentages(
            participantIds = listOf(1L, 2L),
            savedPercentages = mapOf(
                1L to 30.0,
                2L to 40.0,
                3L to 30.0
            )
        )
        assertEquals(42.857142857142854, result.getValue(1L))
        assertEquals(57.142857142857146, result.getValue(2L))
    }
    @Test
    fun resolveDefaultSplitPercentages_mantiene_a_cero_los_nuevos_si_el_total_existente_ya_es_100() {
        val result = resolveDefaultSplitPercentages(
            participantIds = listOf(1L, 2L, 3L),
            savedPercentages = mapOf(
                1L to 60.0,
                2L to 40.0
            )
        )
        assertEquals(60.0, result.getValue(1L))
        assertEquals(40.0, result.getValue(2L))
        assertEquals(0.0, result.getValue(3L))
    }
    @Test
    fun resolveDefaultSelectedParticipantIds_se_queda_con_los_que_tienen_porcentaje() {
        val result = resolveDefaultSelectedParticipantIds(
            participantIds = listOf(1L, 2L, 3L),
            percentages = mapOf(1L to 60.0, 2L to 40.0, 3L to 0.0)
        )
        assertEquals(setOf(1L, 2L), result)
    }
    @Test
    fun resolveDefaultCustomAmounts_convierte_porcentajes_en_importes_y_cierra_el_redondeo() {
        val result = resolveDefaultCustomAmounts(
            totalAmount = 100.0,
            participantIds = listOf(1L, 2L, 3L),
            percentages = mapOf(1L to 33.33, 2L to 33.33, 3L to 33.34)
        )
        assertEquals(33.33, result.getValue(1L))
        assertEquals(33.33, result.getValue(2L))
        assertEquals(33.34, result.getValue(3L))
    }
}
