package com.browntowndev.liftlab.core.domain.useCase.metrics

import com.browntowndev.liftlab.core.domain.models.metrics.ConfiguredMetricsState
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GetConfiguredMetricsStateFlowUseCase(
    private val programsRepository: ProgramsRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val liftsRepository: LiftsRepository,
    private val liftMetricChartsRepository: LiftMetricChartsRepository,
    private val volumeMetricChartsRepository: VolumeMetricChartsRepository,
    private val getGroupedLiftMetricChartDataUseCase: GetGroupedLiftMetricChartDataUseCase,
    private val getGroupedVolumeMetricChartDataUseCase: GetGroupedVolumeMetricChartDataUseCase,
) {
    operator fun invoke(): Flow<ConfiguredMetricsState> {
        // 1. Source Data Flows
        val activeProgramFlow = programsRepository.getActiveProgramFlow()
        val liftsFlow = liftsRepository.getAllFlow()
        val workoutLogsFlow = workoutLogRepository.getAllFlow()
        val liftMetricChartsFlow = liftMetricChartsRepository.getAllFlow()
        val volumeMetricChartsFlow = volumeMetricChartsRepository.getAllFlow()

        // 2. Only recalculate when a dependency changes
        val groupedVolumeMetricDataFlow = combine(volumeMetricChartsFlow, workoutLogsFlow, liftsFlow) { charts, logs, allLifts ->
            getGroupedVolumeMetricChartDataUseCase(
                volumeMetricCharts = charts,
                workoutLogs = logs,
                lifts = allLifts,
            )
        }

        val groupedLiftMetricChartDataFlow = combine(liftMetricChartsFlow, workoutLogsFlow) { charts, logs ->
            getGroupedLiftMetricChartDataUseCase(
                liftMetricCharts = charts,
                workoutLogs = logs,
            )
        }

        // 3. Combine all flows to build the final State
        val combinedSourceDataFlow = combine(
            activeProgramFlow,
            liftsFlow,
            workoutLogsFlow,
            liftMetricChartsFlow,
            volumeMetricChartsFlow,
        ) { program, lifts, logs, liftCharts, volumeMetricCharts ->
            object {
                val program = program
                val lifts = lifts
                val logs = logs
                val liftCharts = liftCharts
                val volumeMetricCharts = volumeMetricCharts
            }
        }

        return combine(
            groupedVolumeMetricDataFlow,
            groupedLiftMetricChartDataFlow,
            combinedSourceDataFlow
        ) {  volumeMetricData, liftMetricData, sourceData ->
            object {
                val groupedVolumeMetricData = volumeMetricData
                val groupedLiftMetricData = liftMetricData
                val sourceData = sourceData
            }
        }.map { allFlowData ->
            ConfiguredMetricsState(
                activeProgram = allFlowData.sourceData.program,
                lifts = allFlowData.sourceData.lifts,
                workoutLogs = allFlowData.sourceData.logs,
                liftMetricCharts = allFlowData.sourceData.liftCharts,
                volumeMetricCharts = allFlowData.sourceData.volumeMetricCharts,
                volumeMetricChartData = allFlowData.groupedVolumeMetricData,
                liftMetricChartData = allFlowData.groupedLiftMetricData,
            )
        }
    }
}