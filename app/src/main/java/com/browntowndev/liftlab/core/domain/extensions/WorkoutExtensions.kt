package com.browntowndev.liftlab.core.domain.extensions

import androidx.compose.ui.util.fastFlatMap
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.utils.generateFirstCompleteStepSequence
import com.browntowndev.liftlab.core.domain.utils.getPossibleStepSizes

/**
 * Calculates all possible step size options for wave-loading lifts within a workout.
 *
 * This function filters a workout's lifts to find standard lifts using the
 * WAVE_LOADING_PROGRESSION scheme. For each of these lifts, it calculates a map of
 * possible step sizes and the resulting initial rep sequence for that step size.
 *
 * This is useful for allowing a user to choose how their reps will progress over a cycle
 * for a specific lift.
 *
 * @param programDeloadWeek The deload week defined at the program level. This is used as a
 *   fallback if a specific lift does not have its own deload week override.
 * @param liftLevelDeloadsEnabled A flag indicating whether lift-specific deload settings
 *   should be used.
 * @return A map where:
 *   - The key is the `workoutLift.id` (Long).
 *   - The value is another map for that specific lift, where:
 *     - The key is a possible step size (Int).
 *     - The value is the generated sequence of reps (List<Int>) for that step size.
 *
 * Example Return: `{ 101L -> { 2 -> [10, 8, 6], 3 -> [10, 7, 4] } }`
 */
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

/**
 * Recalculates the step size for all standard lifts within a list of workouts.
 *
 * This function iterates through all workouts and their lifts. For each standard lift
 * using the WAVE_LOADING_PROGRESSION scheme, it recalculates the step size. If the
 * recalculated step size differs from the current step size, the lift is updated
 * with the new step size.
 *
 * This is particularly useful when program-level deload settings change, requiring
 * recalculation of step sizes for wave-loading lifts.
 *
 * @param deloadToUseInsteadOfLiftLevel An optional integer representing the deload week to
 *   use for recalculation. If provided, this value overrides any lift-specific deload settings.
 *   If null, the lift's own `deloadWeek` is used.
 * @return A map where the key is the `workoutLift.id` (Long) and the value is the
 *   `StandardWorkoutLift` with the recalculated step size. Only lifts with changed step sizes are included.
 */
fun List<Workout>.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel: Int?): Map<Long, StandardWorkoutLift> {
    return this
        .fastFlatMap { workout ->
            workout.lifts
        }
        .filterIsInstance<StandardWorkoutLift>()
        .mapNotNull { workoutLift ->
            workoutLift.getRecalculatedStepSizeForLift(
                deloadToUseInsteadOfLiftLevel = deloadToUseInsteadOfLiftLevel ?: workoutLift.deloadWeek
            ).let { newStepSize ->
                if (workoutLift.stepSize != newStepSize) {
                    workoutLift.id to workoutLift.copy(stepSize = newStepSize)
                } else null
            }
        }.associate { it.first to it.second }
}

/**
 * Recalculates the step size for a single standard lift based on its properties and a potential overriding deload week.
 *
 * This function is specifically for lifts using the `WAVE_LOADING_PROGRESSION` scheme.
 * It determines the possible step sizes based on the lift's rep range and the provided
 * deload week (or the lift's own deload week if not overridden).
 *
 * If the lift's current `stepSize` is among the valid possible step sizes, it's returned.
 * Otherwise, the first possible step size is returned. If no possible step sizes exist,
 * or if the lift does not use wave loading, `null` is returned.
 *
 * @param deloadToUseInsteadOfLiftLevel An optional integer to override the lift's `deloadWeek` for calculation.
 * @return The recalculated step size as an `Int?`, or `null` if not applicable or no valid step size is found.
 */
fun StandardWorkoutLift.getRecalculatedStepSizeForLift(
    deloadToUseInsteadOfLiftLevel: Int?
): Int? {
    return if (progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION) {
        getPossibleStepSizes(
            repRangeTop = repRangeTop,
            repRangeBottom = repRangeBottom,
            stepCount = (deloadToUseInsteadOfLiftLevel ?: deloadWeek)?.let { it - 2 },
        ).let { availableStepSizes ->
            if (availableStepSizes.contains(stepSize)) {
                stepSize
            } else {
                availableStepSizes.firstOrNull()
            }
        }
    } else null
}

/**
 * Converts a [StandardWorkoutLift] to a [CustomWorkoutLift].
 *
 * This function creates a list of [StandardSet]s based on the `setCount`, `rpeTarget`,
 * and rep ranges of the [StandardWorkoutLift]. It then constructs a [CustomWorkoutLift]
 * by copying most properties and using the generated sets.
 * @return A new [CustomWorkoutLift] instance.
 */
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

/**
 * Converts a [CustomWorkoutLift] to a [StandardWorkoutLift].
 *
 * This function attempts to derive standard lift properties (like rep ranges and RPE)
 * from the first set in the [CustomWorkoutLift]'s `customLiftSets`. If no custom sets
 * exist, it uses default values.
 *
 * @return A new [StandardWorkoutLift] instance.
 */
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

