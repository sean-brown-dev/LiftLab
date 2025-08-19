package com.browntowndev.liftlab.core.domain.useCase.progression

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkoutLift

class CalculateLoggingWorkoutUseCase {
    operator fun invoke(
        workout: CalculationWorkout,
        previousSetResults: List<SetResult>,
        previousResultsForDisplay: List<SetResult>,
        microCycle: Int,
        programDeloadWeek: Int,
        useLiftSpecificDeloading: Boolean,
        onlyUseResultsForLiftsInSamePosition: Boolean,
    ): LoggingWorkout {
        var loggingWorkout = LoggingWorkout(
            id = workout.id,
            lifts = listOf()
        )

        workout.lifts
            .filter { it.setCount > 0 } // Shouldn't be able to set this to 0, but just in case
            .fastForEach { workoutLift ->
                val deloadWeek =
                    if (useLiftSpecificDeloading) workoutLift.deloadWeek else programDeloadWeek
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
                            ProgressionScheme.WAVE_LOADING_PROGRESSION -> WaveLoadingProgressionCalculator(
                                programDeloadWeek,
                                microCycle
                            )
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
                                deloadWeek = deloadWeek,
                                incrementOverride = workoutLift.incrementOverride,
                                position = workoutLift.position,
                                progressionScheme = workoutLift.progressionScheme,
                                sets = sets,
                                isCustom = workoutLift is CalculationCustomWorkoutLift
                            )
                        )
                    }
                )
            }

        return loggingWorkout
    }
}