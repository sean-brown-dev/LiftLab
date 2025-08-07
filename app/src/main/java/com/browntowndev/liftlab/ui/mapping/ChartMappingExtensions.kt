package com.browntowndev.liftlab.ui.mapping

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.domain.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.domain.models.metrics.LiftMetricChart
import com.browntowndev.liftlab.core.domain.models.metrics.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.ui.models.metrics.LiftMetricChartModel
import com.browntowndev.liftlab.ui.models.metrics.VolumeMetricChartModel
import com.browntowndev.liftlab.ui.models.metrics.getIntensityChartModel
import com.browntowndev.liftlab.ui.models.metrics.getOneRepMaxChartModel
import com.browntowndev.liftlab.ui.models.metrics.getPerMicrocycleVolumeChartModel
import com.browntowndev.liftlab.ui.models.metrics.getPerWorkoutVolumeChartModel
import com.browntowndev.liftlab.ui.models.workout.displayName
import com.browntowndev.liftlab.ui.models.workoutLogging.WorkoutLogEntryUiModel

object ChartMappingExtensions {
    /**
     * Maps the grouped data from the domain layer into a list of UI-ready chart models.
     */
    fun List<LiftMetricChart>.toChartModels(
        groupedLogs: Map<Long, List<WorkoutLogEntryUiModel>>
    ): List<LiftMetricChartModel> {
        return groupBy { it.liftId }.flatMap { (liftId, chartsForLift) ->
            val resultsForLift = groupedLogs[liftId] ?: emptyList()

            // Build all the selected charts for the liftEntity
            val liftName = resultsForLift.lastOrNull()?.setLogEntries?.firstOrNull()?.liftName
            if (liftName != null && resultsForLift.isNotEmpty()) {
                chartsForLift.fastMap { chart ->
                    LiftMetricChartModel(
                        id = chart.id,
                        liftName = liftName,
                        type = chart.chartType,
                        chartModel = when (chart.chartType) {
                            LiftMetricChartType.ESTIMATED_ONE_REP_MAX -> getOneRepMaxChartModel(
                                resultsForLift,
                                setOf()
                            )

                            LiftMetricChartType.VOLUME -> getPerWorkoutVolumeChartModel(
                                resultsForLift,
                                setOf()
                            )

                            LiftMetricChartType.RELATIVE_INTENSITY -> getIntensityChartModel(
                                resultsForLift,
                                setOf()
                            )
                        }
                    )
                }.fastMapNotNull { chartModel ->
                    if (chartModel.chartModel.chartEntryModel != null) chartModel else null
                }
            } else {
                emptyList()
            }
        }.sortedBy { it.liftName }
    }

    /**
     * Maps the grouped data from the domain layer into a list of UI-ready volume chart models.
     */
    fun List<Lift>.toVolumeMetricChartModels(
        groupedData: Map<VolumeMetricChart, List<WorkoutLogEntryUiModel>>,
    ): List<VolumeMetricChartModel> {
        val secondaryVolumeTypesById = associate { it.id to it.secondaryVolumeTypesBitmask }
        return groupedData.mapNotNull { (chart, logs) ->
            if (logs.isNotEmpty()) {
                VolumeMetricChartModel(
                    id = chart.id,
                    volumeType = chart.volumeType.displayName(),
                    volumeTypeImpact = chart.volumeTypeImpact.displayName(),
                    chartModel = getPerMicrocycleVolumeChartModel(
                        workoutLogs = logs,
                        secondaryVolumeTypesByLiftId = if (chart.volumeTypeImpact != VolumeTypeImpact.PRIMARY)
                            secondaryVolumeTypesById else null
                    )
                )
            } else {
                null
            }
        }.sortedBy { it.volumeType }
    }
}