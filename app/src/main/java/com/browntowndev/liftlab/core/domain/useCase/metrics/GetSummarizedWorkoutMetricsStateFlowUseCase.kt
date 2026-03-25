package com.browntowndev.liftlab.core.domain.useCase.metrics

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.models.metrics.AllWorkoutTopSets
import com.browntowndev.liftlab.core.domain.models.metrics.LiftId
import com.browntowndev.liftlab.core.domain.models.metrics.SummarizedWorkoutMetricsState
import com.browntowndev.liftlab.core.domain.models.metrics.WorkoutLogId
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.domain.utils.WeightCalculationUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetSummarizedWorkoutMetricsStateFlowUseCase(
    private val workoutLogRepository: WorkoutLogRepository,
) {
    operator fun invoke(): Flow<SummarizedWorkoutMetricsState> {
        return workoutLogRepository.getAllFlow()
            .map { workoutLogs ->
                val dateOrderedWorkoutLogsWithPersonalRecords = sortAndSetPersonalRecords(workoutLogs)
                val topSets = getTopSets(dateOrderedWorkoutLogsWithPersonalRecords)
                SummarizedWorkoutMetricsState(
                    dateOrderedWorkoutLogsWithPersonalRecords = dateOrderedWorkoutLogsWithPersonalRecords,
                    topSets = topSets
                )
            }
    }

    private fun sortAndSetPersonalRecords(workoutLogs: List<WorkoutLogEntry>): List<WorkoutLogEntry> {
        val personalRecords = getPersonalRecords(workoutLogs)
        val updatedLogs = workoutLogs
            .sortedByDescending { it.date }
            .fastMap { workoutLog ->
                workoutLog.copy(
                    setLogEntries = workoutLog.setLogEntries
                        .sortedWith(
                            compareBy<SetLogEntry> { it.liftPosition }
                                .thenBy { it.setPosition }
                                .thenBy { it.myoRepSetPosition ?: -1 }
                        )
                        .fastMap { setLog ->
                            if (personalRecords.contains(setLog)) {
                                setLog.copy(
                                    isPersonalRecord = true
                                )
                            } else setLog
                        }
                )
            }

        return updatedLogs
    }

    private fun getPersonalRecords(workoutLogs: List<WorkoutLogEntry>): HashSet<SetLogEntry> {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setLogEntries
        }.groupBy { result ->
            result.liftId
        }.map { liftSetResults ->
            liftSetResults.value.maxBy {
                // Set to a non-zero weight so 1RM gets calculated
                val weight = if (it.weight == 0f) 1f else it.weight
                WeightCalculationUtils.getOneRepMax(weight, it.reps, it.rpe)
            }
        }.toHashSet()
    }

    private fun getTopSets(workoutLogs: List<WorkoutLogEntry>): AllWorkoutTopSets {
        return workoutLogs.associate { workoutLog ->
            WorkoutLogId(workoutLog.id) to getTopSetsForWorkout(workoutLog)
        }.let { topSets ->
            AllWorkoutTopSets(topSets)
        }
    }

    private fun getTopSetsForWorkout(workoutLog: WorkoutLogEntry): AllWorkoutTopSets.WorkoutTopSets {
        return workoutLog.setLogEntries
            .groupBy { LiftId(it.liftId) }
            .filterValues { set -> set.isNotEmpty() }
            .mapValues { (_, sets) ->
                val setSize = sets.size
                val topSet = sets.maxBy {
                    WeightCalculationUtils.getOneRepMax(it.weight, it.reps, it.rpe)
                }
                AllWorkoutTopSets.WorkoutTopSets.TopSet(
                    setCount = setSize,
                    setLog = topSet
                )
            }.let { topSets ->
                AllWorkoutTopSets.WorkoutTopSets(topSets)
            }
    }
}