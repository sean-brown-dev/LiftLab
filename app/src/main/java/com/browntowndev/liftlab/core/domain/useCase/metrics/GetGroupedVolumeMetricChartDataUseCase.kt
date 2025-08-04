package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.domain.enums.getVolumeTypes
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry

class GetGroupedVolumeMetricChartDataUseCase {
    /**
     * @param volumeMetricCharts The definitions for the charts to generate.
     * @param workoutLogs The complete history of workout logs.
     * @param lifts The complete library of lifts, used to determine volume types.
     * @return A map where the key is the VolumeMetricChart and the value is a list
     *         of WorkoutLogEntries that have been filtered to only contain sets
     *         relevant to that chart.
     */
    operator fun invoke(
        volumeMetricCharts: List<VolumeMetricChart>,
        workoutLogs: List<WorkoutLogEntry>,
        lifts: List<Lift>,
    ): Map<VolumeMetricChart, List<WorkoutLogEntry>> {
        if (volumeMetricCharts.isEmpty() || workoutLogs.isEmpty() || lifts.isEmpty()) return emptyMap()

        // 1. Pre-computation: Create lookup maps for fast filtering.
        val primaryLiftsByVolumeType = createLiftLookup(lifts) { it.volumeTypesBitmask }
        val secondaryLiftsByVolumeType = createLiftLookup(lifts) { it.secondaryVolumeTypesBitmask }

        // 2. Process each chart
        return volumeMetricCharts.associateWith { chart ->
            // Determine the set of lift IDs relevant for the current chart
            val relevantLiftIds = when (chart.volumeTypeImpact) {
                VolumeTypeImpact.PRIMARY -> primaryLiftsByVolumeType[chart.volumeType].orEmpty()
                VolumeTypeImpact.SECONDARY -> secondaryLiftsByVolumeType[chart.volumeType].orEmpty()
                VolumeTypeImpact.COMBINED -> {
                    val primary = primaryLiftsByVolumeType[chart.volumeType].orEmpty()
                    val secondary = secondaryLiftsByVolumeType[chart.volumeType].orEmpty()
                    primary + secondary
                }
            }

            if (relevantLiftIds.isEmpty()) return@associateWith emptyList()

            // Filter workout logs, creating new copies with only relevant sets
            workoutLogs.mapNotNull { log ->
                val relevantSets = log.setResults.filter { it.liftId in relevantLiftIds }
                if (relevantSets.isNotEmpty()) {
                    log.copy(setResults = relevantSets)
                } else {
                    null
                }
            }
        }
    }

    /**
     * Helper to create a mapping from a VolumeType to a Set of lift IDs.
     * Using a Set for lift IDs allows for O(1) average time complexity for checks.
     */
    private fun createLiftLookup(
        lifts: List<Lift>,
        bitmaskSelector: (Lift) -> Int?
    ): Map<VolumeType, Set<Long>> {
        val lookup = mutableMapOf<VolumeType, MutableSet<Long>>()
        lifts.forEach { lift ->
            val bitmask = bitmaskSelector(lift)
            bitmask?.getVolumeTypes()?.forEach { volumeType ->
                lookup.getOrPut(volumeType) { mutableSetOf() }.add(lift.id)
            }
        }
        return lookup
    }
}