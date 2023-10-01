package com.browntowndev.liftlab.core.progression

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

class StandardProgressionFactory: ProgressionFactory {
    override fun calculate(
        workout: WorkoutDto,
        previousSetResults: List<SetResult>,
        microCycle: Int,
        programDeloadWeek: Int,
    ): LoggingWorkoutDto {
        var loggingWorkout = LoggingWorkoutDto(
            id = workout.id,
            name = workout.name,
            lifts = listOf()
        )

        workout.lifts.fastForEach { workoutLift ->
            loggingWorkout = loggingWorkout.copy(
                lifts = loggingWorkout.lifts.toMutableList().apply {
                    val isDeloadWeek = (microCycle + 1) == (workoutLift.deloadWeek ?: programDeloadWeek)
                    add(
                        LoggingWorkoutLiftDto(
                            id = workoutLift.id,
                            liftId = workoutLift.liftId,
                            liftName = workoutLift.liftName,
                            liftMovementPattern = workoutLift.liftMovementPattern,
                            liftVolumeTypes = workoutLift.liftVolumeTypes,
                            liftSecondaryVolumeTypes = workoutLift.liftSecondaryVolumeTypes,
                            liftRestTime = workoutLift.liftRestTime,
                            deloadWeek = workoutLift.deloadWeek,
                            incrementOverride = workoutLift.incrementOverride,
                            liftIncrementOverride = workoutLift.liftIncrementOverride,
                            position = workoutLift.position,
                            progressionScheme = workoutLift.progressionScheme,
                            restTime = workoutLift.restTime,
                            setCount = workoutLift.setCount,
                            sets = when (workoutLift.progressionScheme) {
                                ProgressionScheme.DOUBLE_PROGRESSION -> DoubleProgressionCalculator()
                                ProgressionScheme.LINEAR_PROGRESSION -> LinearProgressionCalculator()
                                ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> DynamicDoubleProgressionCalculator()
                                ProgressionScheme.WAVE_LOADING_PROGRESSION -> WaveLoadingProgressionCalculator(programDeloadWeek)
                            }.calculate(
                                workoutLift = workoutLift,
                                previousSetResults = previousSetResults,
                                isDeloadWeek = isDeloadWeek,
                            )
                        )
                    )
                }
            )
        }

        return loggingWorkout
    }
}