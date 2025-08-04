package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.models.workout.LiftWithHistoryState
import com.browntowndev.liftlab.core.domain.models.workoutLogging.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.util.Date

class GetLiftWithHistoryStateFlowUseCase(
    private val liftsRepository: LiftsRepository,
    private val workoutLogRepository: WorkoutLogRepository,
) {
    operator fun invoke(liftId: Long?): Flow<LiftWithHistoryState> {
        // Null when creating new lift
        if (liftId == null) return flowOf(LiftWithHistoryState())

        val liftFlow = liftsRepository.getByIdFlow(liftId)
        val workoutLogFlow = workoutLogRepository.getWorkoutLogsForLiftFlow(liftId)

        return combine(
            liftFlow,
            workoutLogFlow,
        ) { lift, workoutLogEntries ->
            LiftWithHistoryState(
                lift = lift ?: throw IllegalArgumentException("Lift with id $liftId not found"),
                workoutLogEntries = workoutLogEntries,
                maxVolume = getMaxVolume(workoutLogEntries),
                maxWeight = getMaxWeight(workoutLogEntries),
                topTenPerformances = getTopTenPerformances(workoutLogEntries),
                totalReps = getTotalReps(workoutLogEntries),
                totalVolume = getTotalVolume(workoutLogEntries),
            )
        }
    }

    private fun getMaxVolume(workoutLogs: List<WorkoutLogEntry>): Pair<Date, Float>? {
        return workoutLogs.fastMap { workoutLog ->
            workoutLog.date to
                    workoutLog.setResults.maxOf {
                        it.reps * it.weight
                    }
        }.maxByOrNull { it.second }
    }


    private fun getMaxWeight(workoutLogs: List<WorkoutLogEntry>): Pair<Date, Float>? {
        return workoutLogs.fastMap { workoutLog ->
            workoutLog.date to
                    workoutLog.setResults.maxOf {
                        it.weight
                    }
        }.maxByOrNull { it.second }
    }

    private fun getTopTenPerformances(workoutLogs: List<WorkoutLogEntry>): List<Pair<Date, SetLogEntry>> {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                workoutLog.date to setLog
            }
        }.sortedByDescending { it.second.oneRepMax }.take(10)
    }

    private fun getTotalReps(workoutLogs: List<WorkoutLogEntry>): Int {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps
            }
        }.sum()
    }

    private fun getTotalVolume(workoutLogs: List<WorkoutLogEntry>): Float {
        return workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps * setLog.weight
            }
        }.sum()
    }
}