package com.browntowndev.liftlab.core.domain.extensions

import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.generateFirstCompleteStepSequence
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getPossibleStepSizes
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.StandardSet
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet

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

fun StandardWorkoutLift.convertToCustomWorkoutLift(): CustomWorkoutLift {
    val customSets = mutableListOf<GenericLiftSet>()
    for (i in 0 until setCount) {
        customSets.add(
            StandardSet(
                workoutLiftId = id,
                position = i,
                rpeTarget = rpeTarget,
                repRangeBottom = repRangeBottom,
                repRangeTop = repRangeTop
            )
        )
    }

    return CustomWorkoutLift(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftName = liftName,
        liftMovementPattern = liftMovementPattern,
        liftVolumeTypes = liftVolumeTypes,
        liftSecondaryVolumeTypes = liftSecondaryVolumeTypes,
        position = position,
        setCount = setCount,
        progressionScheme = progressionScheme,
        deloadWeek = deloadWeek,
        liftNote = null,
        incrementOverride = incrementOverride,
        restTime = restTime,
        restTimerEnabled = restTimerEnabled,
        customLiftSets = customSets
    )
}

fun CustomWorkoutLift.convertToStandardWorkoutLift(): StandardWorkoutLift {
    val topCustomLiftSet = customLiftSets.firstOrNull()
    return StandardWorkoutLift(
        id = id,
        workoutId = workoutId,
        liftId = liftId,
        liftName = liftName,
        liftMovementPattern = liftMovementPattern,
        liftVolumeTypes = liftVolumeTypes,
        liftSecondaryVolumeTypes = liftSecondaryVolumeTypes,
        deloadWeek = deloadWeek,
        liftNote = null,
        position = position,
        setCount = setCount,
        repRangeBottom = topCustomLiftSet?.repRangeBottom ?: 8,
        repRangeTop = topCustomLiftSet?.repRangeTop ?: 10,
        rpeTarget = topCustomLiftSet?.rpeTarget ?: 8f,
        incrementOverride = null,
        restTime = restTime,
        restTimerEnabled = restTimerEnabled,
        progressionScheme = progressionScheme,
    )
}