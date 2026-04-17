package com.example.divvyup.integration.ui

import kotlin.math.round

private fun roundTo2Decimals(value: Double): Double = round(value * 100.0) / 100.0

fun resolveDefaultSplitPercentages(
    participantIds: List<Long>,
    savedPercentages: Map<Long, Double>
): Map<Long, Double> {
    val currentIds = participantIds.distinct()
    if (currentIds.isEmpty()) return emptyMap()

    val sanitized = currentIds.associateWith { participantId ->
        savedPercentages[participantId]
            ?.takeIf { it.isFinite() && it >= 0.0 }
            ?: 0.0
    }

    val total = sanitized.values.sum()
    if (total <= 0.0) {
        val equalShare = 100.0 / currentIds.size
        return currentIds.associateWith { equalShare }
    }

    return if (kotlin.math.abs(total - 100.0) < 0.01) {
        sanitized
    } else {
        currentIds.associateWith { participantId ->
            sanitized.getValue(participantId) * 100.0 / total
        }
    }
}

fun resolveDefaultSelectedParticipantIds(
    participantIds: List<Long>,
    percentages: Map<Long, Double>
): Set<Long> {
    val currentIds = participantIds.distinct()
    if (currentIds.isEmpty()) return emptySet()

    val selected = currentIds.filter { participantId ->
        (percentages[participantId] ?: 0.0) > 0.0
    }.toSet()

    return selected.ifEmpty { currentIds.toSet() }
}

fun resolveDefaultCustomAmounts(
    totalAmount: Double,
    participantIds: List<Long>,
    percentages: Map<Long, Double>
): Map<Long, Double> {
    val currentIds = participantIds.distinct()
    if (currentIds.isEmpty()) return emptyMap()
    if (totalAmount <= 0.0) return currentIds.associateWith { 0.0 }

    val normalizedPercentages = resolveDefaultSplitPercentages(currentIds, percentages)
    var assigned = 0.0

    return currentIds.mapIndexed { index, participantId ->
        val amount = if (index == currentIds.lastIndex) {
            roundTo2Decimals(totalAmount - assigned)
        } else {
            roundTo2Decimals(totalAmount * (normalizedPercentages[participantId] ?: 0.0) / 100.0)
                .also { assigned += it }
        }
        participantId to amount
    }.toMap()
}

