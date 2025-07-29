package com.browntowndev.liftlab.core.domain.extensions

import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.generateFirstCompleteStepSequence
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getPossibleStepSizes
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout

fun Workout.getRecalculatedWorkoutLiftStepSizeOptions(
    programDeloadWeek: Int,
    liftLevelDeloadsEnabled: Boolean,
): Map<Long, Map<Int, List<Int>>> {
    return lifts
        .filterIsInstance<StandardWorkoutLift>()
        .filter { it.progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION }
        .associate { workoutLift ->
            workoutLift.id to getPossibleStepSizes(
                repRangeTop = workoutLift.repRangeTop,
                repRangeBottom = workoutLift.repRangeBottom,
                stepCount = (if (liftLevelDeloadsEnabled) workoutLift.deloadWeek else programDeloadWeek)?.let { it - 2 }
            ).associateWith { option ->
                generateFirstCompleteStepSequence(
                    repRangeTop = workoutLift.repRangeTop,
                    repRangeBottom = workoutLift.repRangeBottom,
                    stepSize = option
                )
            }
        }
}