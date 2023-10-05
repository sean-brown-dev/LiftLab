package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.toSimpleDateString
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.OneRepMaxResultDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.progression.CalculationEngine

data class LiftDetailsState(
    val lift: LiftDto? = null,
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val volumeTypeDisplayNames: List<String> = listOf(),
    val secondaryVolumeTypeDisplayNames: List<String> = listOf(),
) {
    val oneRepMax by lazy {
        workoutLogs.fastMap { workoutLog ->
            Pair(
                workoutLog.date.toSimpleDateString(),
                workoutLog.setResults.maxOf {
                    CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                })
        }.maxByOrNull { it.second }
    }

    val maxVolume by lazy {
        workoutLogs.fastMap { workoutLog ->
            Pair(
                workoutLog.date.toSimpleDateString(),
                workoutLog.setResults.maxOf {
                    it.reps * it.weight
                }
            )
        }.maxByOrNull { it.second }
    }

    val maxWeight by lazy {
        workoutLogs.fastMap { workoutLog ->
            Pair(
                workoutLog.date.toSimpleDateString(),
                workoutLog.setResults.maxOf {
                    it.weight
                }
            )
        }.maxByOrNull { it.second }
    }

    val topTenPerformances by lazy {
        workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                OneRepMaxResultDto(
                    setsAndRepsLabel = "${setLog.weight}x${setLog.reps} @${setLog.rpe}",
                    date = workoutLog.date.toSimpleDateString(),
                    oneRepMax = CalculationEngine.getOneRepMax(setLog.weight, setLog.reps, setLog.rpe)
                )
            }
        }.sortedByDescending { it.oneRepMax }.take(10)
    }

    val totalReps by lazy {
        workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps
            }
        }.sum()
    }

    val totalVolume by lazy {
        workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps * setLog.weight
            }
        }.sum()
    }
}