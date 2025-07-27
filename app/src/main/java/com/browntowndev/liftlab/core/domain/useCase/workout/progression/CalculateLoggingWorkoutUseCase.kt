package com.browntowndev.liftlab.core.domain.useCase.workout.progression

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.LoggingWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult

class CalculateLoggingWorkoutUseCase {
    fun calculate(
        workout: Workout,
        previousSetResults: List<SetResult>,
        previousResultsForDisplay: List<SetResult>,
        microCycle: Int,
        programDeloadWeek: Int,
        useLiftSpecificDeloading: Boolean,
        onlyUseResultsForLiftsInSamePosition: Boolean,
    ): LoggingWorkout {
        var loggingWorkout = LoggingWorkout(
            id = workout.id,
            name = workout.name,
            lifts = listOf()
        )

        workout.lifts.fastForEach { workoutLift ->
            val deloadWeek = if (useLiftSpecificDeloading) workoutLift.deloadWeek else programDeloadWeek
            val isDeloadWeek = (microCycle + 1) == deloadWeek
            val resultsForLift = previousSetResults.filter { result ->
                (!onlyUseResultsForLiftsInSamePosition || result.liftPosition == workoutLift.position)
                        && result.liftId == workoutLift.liftId
            }
            val displayResultsForLift = previousResultsForDisplay.filter { result ->
                (!onlyUseResultsForLiftsInSamePosition || result.liftPosition == workoutLift.position)
                        && result.liftId == workoutLift.liftId
            }

            loggingWorkout = loggingWorkout.copy(
                lifts = loggingWorkout.lifts.toMutableList().apply {
                    val sets = when (workoutLift.progressionScheme) {
                        ProgressionScheme.DOUBLE_PROGRESSION -> DoubleProgressionCalculator()
                        ProgressionScheme.LINEAR_PROGRESSION -> LinearProgressionCalculator()
                        ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> DynamicDoubleProgressionCalculator()
                        ProgressionScheme.WAVE_LOADING_PROGRESSION -> WaveLoadingProgressionCalculator(programDeloadWeek, microCycle)
                    }.calculate(
                        workoutLift = workoutLift,
                        previousSetResults = resultsForLift,
                        previousResultsForDisplay = displayResultsForLift,
                        isDeloadWeek = isDeloadWeek,
                    )
                    
                    add(
                        LoggingWorkoutLift(
                            id = workoutLift.id,
                            liftId = workoutLift.liftId,
                            liftName = workoutLift.liftName,
                            liftMovementPattern = workoutLift.liftMovementPattern,
                            liftVolumeTypes = workoutLift.liftVolumeTypes,
                            liftSecondaryVolumeTypes = workoutLift.liftSecondaryVolumeTypes,
                            deloadWeek = deloadWeek,
                            incrementOverride = workoutLift.incrementOverride,
                            position = workoutLift.position,
                            progressionScheme = workoutLift.progressionScheme,
                            restTime = workoutLift.restTime,
                            restTimerEnabled = workoutLift.restTimerEnabled,
                            setCount = sets.size,
                            note = workoutLift.liftNote,
                            sets = sets,
                        )
                    )
                }
            )
        }

        return loggingWorkout
    }
}