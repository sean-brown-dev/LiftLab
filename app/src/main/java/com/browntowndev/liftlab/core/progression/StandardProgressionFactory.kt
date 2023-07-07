package com.browntowndev.liftlab.core.progression

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.dtos.ProgressionDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutWithProgressionDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

class StandardProgressionFactory: ProgressionFactory {
    override fun calculate(
        programDeloadWeek: Int,
        workout: WorkoutDto,
        previousSetResults: List<SetResult>
    ): WorkoutWithProgressionDto {
        val progressionByWorkout = HashMap<Long, List<ProgressionDto>>()
        workout.lifts.fastForEach { workoutLift ->
            progressionByWorkout[workoutLift.id] = when (workoutLift.progressionScheme) {
                ProgressionScheme.DOUBLE_PROGRESSION -> DoubleProgressionCalculator()
                ProgressionScheme.LINEAR_PROGRESSION -> LinearProgressionCalculator()
                ProgressionScheme.DYNAMIC_DOUBLE_PROGRESSION -> DynamicDoubleProgressionCalculator()
                ProgressionScheme.WAVE_LOADING_PROGRESSION -> WaveLoadingProgressionCalculator(programDeloadWeek)
            }.calculate(workoutLift = workoutLift, previousSetResults = previousSetResults)
        }

        return WorkoutWithProgressionDto(
            workout = workout,
            progressions = progressionByWorkout
        )
    }
}