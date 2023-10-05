package com.browntowndev.liftlab.ui.viewmodels.states

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.isWholeNumber
import com.browntowndev.liftlab.core.common.toSimpleDateString
import com.browntowndev.liftlab.core.persistence.dtos.LiftDto
import com.browntowndev.liftlab.core.persistence.dtos.OneRepMaxResultDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.progression.CalculationEngine
import java.text.NumberFormat
import kotlin.math.roundToInt

data class LiftDetailsState(
    val lift: LiftDto? = null,
    val workoutLogs: List<WorkoutLogEntryDto> = listOf(),
    val volumeTypeDisplayNames: List<String> = listOf(),
    val secondaryVolumeTypeDisplayNames: List<String> = listOf(),
) {
    val oneRepMax: Pair<String, String>? by lazy {
        val oneRepMax = workoutLogs.fastMap { workoutLog ->
            Pair(
                workoutLog.date.toSimpleDateString(),
                workoutLog.setResults.maxOf {
                    CalculationEngine.getOneRepMax(it.weight, it.reps, it.rpe)
                })
        }.maxByOrNull { it.second }

        if (oneRepMax != null) {
            Pair(oneRepMax.first, oneRepMax.second.toString())
        } else null
    }

    val maxVolume: Pair<String, String>? by lazy {
        val maxVolume = workoutLogs.fastMap { workoutLog ->
            Pair(
                workoutLog.date.toSimpleDateString(),
                workoutLog.setResults.maxOf {
                    it.reps * it.weight
                }
            )
        }.maxByOrNull { it.second }

        if (maxVolume != null) {
            Pair(maxVolume.first, formatFloatString(maxVolume.second))
        } else null
    }

    val maxWeight: Pair<String, String>? by lazy {
        val maxWeight = workoutLogs.fastMap { workoutLog ->
            Pair(
                workoutLog.date.toSimpleDateString(),
                workoutLog.setResults.maxOf {
                    it.weight
                }
            )
        }.maxByOrNull { it.second }

        if (maxWeight != null) {
            Pair(maxWeight.first, formatFloatString(maxWeight.second))
        } else null
    }

    val topTenPerformances: List<OneRepMaxResultDto> by lazy {
        workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                OneRepMaxResultDto(
                    setsAndRepsLabel = "${formatFloatString(setLog.weight)}x${setLog.reps} @${setLog.rpe}",
                    date = workoutLog.date.toSimpleDateString(),
                    oneRepMax = CalculationEngine.getOneRepMax(setLog.weight, setLog.reps, setLog.rpe).toString()
                )
            }
        }.sortedByDescending { it.oneRepMax }.take(10)
    }

    val totalReps: String by lazy {
        workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps
            }
        }.sum().toString()
    }

    val totalVolume: String by lazy {
        formatFloatString(workoutLogs.flatMap { workoutLog ->
            workoutLog.setResults.map { setLog ->
                setLog.reps * setLog.weight
            }
        }.sum())
    }

    private fun formatFloatString(float: Float): String {
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.maximumFractionDigits = 2

        return if (float.isWholeNumber()) numberFormat.format(float.roundToInt())
        else numberFormat.format(float)
    }
}