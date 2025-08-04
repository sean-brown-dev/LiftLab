package com.browntowndev.liftlab.core.domain.extensions

import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.generateFirstCompleteStepSequence
import com.browntowndev.liftlab.core.common.Utils.StepSize.Companion.getPossibleStepSizes
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet

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

/**
 * Transforms a GenericLiftSet to a different set type based on newSetType.
 * Preserves common properties and uses sensible defaults for new ones.
 *
 * @param newSetType The target SetType to transform into.
 * @param defaultDropPercentage The default weight drop percentage to use when creating a DropSet.
 * @param defaultMyoRepFloor The default minimum reps for a MyoRepSet's activation set.
 * @param defaultMyoSetGoal The default number of back-off sets for a MyoRepSet.
 * @param defaultStandardRpe The default RPE to use when converting from another type to a StandardSet.
 * @return A new GenericLiftSet instance of the target type.
 */
fun GenericLiftSet.transformToType(
    newSetType: SetType,
    defaultDropPercentage: Float = 0.1f,
    defaultMyoRepFloor: Int = 5,
    defaultMyoSetGoal: Int = 3,
    defaultStandardRpe: Float = 8.0f
): GenericLiftSet {
    // If we are transforming to the same type, just return the original object.
    val currentType = when (this) {
        is StandardSet -> SetType.STANDARD
        is DropSet -> SetType.DROP_SET
        is MyoRepSet -> SetType.MYOREP
        else -> null // Handle unknown types
    }
    if (newSetType == currentType) return this

    return when (this) {
        is StandardSet ->
            when (newSetType) {
                SetType.DROP_SET -> DropSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    dropPercentage = defaultDropPercentage,
                    rpeTarget = this.rpeTarget,
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
                SetType.MYOREP -> MyoRepSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    repFloor = defaultMyoRepFloor,
                    repRangeTop = this.repRangeTop,
                    repRangeBottom = this.repRangeBottom,
                    rpeTarget = this.rpeTarget,
                    setGoal = defaultMyoSetGoal,
                )
                SetType.STANDARD -> this // Already handled above, but necessary to compile
            }
        is MyoRepSet ->
            when (newSetType) {
                SetType.DROP_SET -> DropSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    dropPercentage = defaultDropPercentage,
                    rpeTarget = this.rpeTarget, // Prefer inheriting RPE
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
                SetType.STANDARD -> StandardSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    rpeTarget = defaultStandardRpe,
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
                SetType.MYOREP -> this // Already handled above
            }
        is DropSet ->
            when (newSetType) {
                SetType.MYOREP -> MyoRepSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    repFloor = defaultMyoRepFloor,
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                    rpeTarget = this.rpeTarget,
                    setGoal = defaultMyoSetGoal,
                )
                SetType.STANDARD -> StandardSet(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    rpeTarget = defaultStandardRpe,
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
                SetType.DROP_SET -> this // Already handled above
            }
        else -> throw IllegalArgumentException("${this::class.simpleName} is not a recognized custom set type.")
    }
}
