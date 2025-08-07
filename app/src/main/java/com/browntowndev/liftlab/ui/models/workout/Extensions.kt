package com.browntowndev.liftlab.ui.models.workout

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.appendSuperscript
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpact
import com.browntowndev.liftlab.core.domain.enums.displayName
import com.browntowndev.liftlab.core.domain.enums.getVolumeTypes
import com.browntowndev.liftlab.core.domain.utils.generateFirstCompleteStepSequence
import com.browntowndev.liftlab.core.domain.utils.getPossibleStepSizes
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingDropSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingMyoRepSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingStandardSetUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutLiftUiModel
import com.browntowndev.liftlab.ui.models.workoutLogging.LoggingWorkoutUiModel
import java.util.Locale.US
import kotlin.collections.getOrDefault


private fun getVolumeTypeMapForGenericWorkoutLifts(lifts: List<WorkoutLiftUiModel>, impact: VolumeTypeImpact):  HashMap<String, Pair<Float, Boolean>> {
    val volumeCounts = hashMapOf<String, Pair<Float, Boolean>>()
    lifts.fastForEach { lift ->
        val volumeTypes = when(impact) {
            VolumeTypeImpact.PRIMARY -> lift.liftVolumeTypes
            VolumeTypeImpact.SECONDARY -> lift.liftSecondaryVolumeTypes
            VolumeTypeImpact.COMBINED -> lift.liftVolumeTypes + (lift.liftSecondaryVolumeTypes ?: 0)
        }
        val secondaryVolumeTypes = lift.liftSecondaryVolumeTypes?.getVolumeTypes()?.toHashSet()

        volumeTypes?.getVolumeTypes()?.fastForEach { volumeType ->
            val displayName = volumeType.displayName()
            val currTotalVolume: Pair<Float, Boolean>? = volumeCounts.getOrDefault(displayName, null)
            val hasMyoReps = (lift as? CustomWorkoutLiftUiModel)?.customLiftSets?.any { it is MyoRepSetUiModel } ?: false
            var newTotalVolume: Float = if(secondaryVolumeTypes?.contains(volumeType) == true)
                lift.setCount / 2f
            else
                lift.setCount.toFloat()

            if (currTotalVolume != null) {
                newTotalVolume += currTotalVolume.first
            }

            volumeCounts[displayName] = Pair(newTotalVolume, hasMyoReps || currTotalVolume?.second ?: false )
        }
    }

    return volumeCounts
}

private fun getVolumeTypeMapForLoggingWorkoutLifts(lifts: List<LoggingWorkoutLiftUiModel>, impact: VolumeTypeImpact):  HashMap<String, Pair<Int, Boolean>> {
    val volumeCounts = hashMapOf<String, Pair<Int, Boolean>>()
    lifts.fastForEach { lift ->
        val volumeTypes = when(impact) {
            VolumeTypeImpact.PRIMARY -> lift.liftVolumeTypes
            VolumeTypeImpact.SECONDARY -> lift.liftSecondaryVolumeTypes
            VolumeTypeImpact.COMBINED -> lift.liftVolumeTypes + (lift.liftSecondaryVolumeTypes ?: 0)
        }

        volumeTypes?.getVolumeTypes()?.fastForEach { volumeType ->
            val displayName = volumeType.displayName()
            val currTotalVolume: Pair<Int, Boolean>? = volumeCounts.getOrDefault(displayName, null)
            val hasMyoReps = lift.sets.any { it is LoggingMyoRepSetUiModel }
            var newTotalVolume: Int = lift.setCount

            if (currTotalVolume != null) {
                newTotalVolume += currTotalVolume.first
            }

            volumeCounts[displayName] = Pair(newTotalVolume, hasMyoReps || currTotalVolume?.second ?: false )
        }
    }

    return volumeCounts
}

private fun getVolumeTypeLabelsForGenericWorkoutLifts(lifts: List<WorkoutLiftUiModel>, impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeMapForGenericWorkoutLifts(lifts, impact).map { (volumeType, totalVolume) ->
        val volume = if (totalVolume.first % 1.0 == 0.0) {
            String.format(US, "%.0f", totalVolume.first) // No decimals if the value is a whole number
        } else {
            String.format(US, "%.1f", totalVolume.first) // One decimal if there is a non-zero decimal part
        }

        val plainVolumeString = "$volumeType: $volume"
        if(totalVolume.second) plainVolumeString.appendSuperscript("+myo")
        else plainVolumeString
    }
}

/**
 * Calculates and returns a list of human-readable labels representing the volume types and their
 * counts for a given `WorkoutUiModel`.
 *
 * This function leverages `getVolumeTypeLabelsForGenericWorkoutLifts` to perform the
 * calculation, passing in the lifts from the current `WorkoutUiModel`.
 *
 * @param impact Specifies how to consider volume types (PRIMARY, SECONDARY, or COMBINED).
 * @return A list of `CharSequence` objects, where each sequence is a formatted string
 *         like "VolumeTypeName: Count" or "VolumeTypeName: Count+myo" if MyoReps are present.
 *         Example: `["Chest: 3.0", "Shoulders: 1.5+myo"]`
 */
fun WorkoutUiModel.getVolumeTypeLabels(impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeLabelsForGenericWorkoutLifts(this.lifts, impact)
}

private fun getVolumeTypeLabelsForLoggingWorkoutLifts(lifts: List<LoggingWorkoutLiftUiModel>, impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeMapForLoggingWorkoutLifts(lifts, impact).map { (volumeType, volumeData) ->
        val plainVolumeString = "$volumeType: ${volumeData.first}"
        if(volumeData.second) plainVolumeString.appendSuperscript("+myo")
        else plainVolumeString
    }
}

/**
 * Calculates and returns a list of human-readable labels representing the volume types and their
 * counts for a given `LoggingWorkoutUiModel`.
 *
 * This function leverages `getVolumeTypeLabelsForLoggingWorkoutLifts` to perform the
 * calculation, passing in the lifts from the current `LoggingWorkoutUiModel`.
 *
 * @param impact Specifies how to consider volume types (PRIMARY, SECONDARY, or COMBINED).
 * @return A list of `CharSequence` objects, where each sequence is a formatted string
 *         like "VolumeTypeName: Count" or "VolumeTypeName: Count+myo" if MyoReps are present.
 *         Example: `["Chest: 3", "Shoulders: 2+myo"]`
 */
fun LoggingWorkoutUiModel.getVolumeTypeLabels(impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeLabelsForLoggingWorkoutLifts(this.lifts, impact)
}

/**
 * Calculates and returns a list of human-readable labels representing the aggregated volume types
 * and their counts across all workouts in a `ProgramUiModel`.
 *
 * @param impact Specifies how to consider volume types (PRIMARY, SECONDARY, or COMBINED).
 * @return A list of `CharSequence` objects, where each sequence is a formatted string
 *         like "VolumeTypeName: Count" or "VolumeTypeName: Count+myo" if MyoReps are present.
 */
fun ProgramUiModel.getVolumeTypeLabels(impact: VolumeTypeImpact): List<CharSequence> {
    return getVolumeTypeLabelsForGenericWorkoutLifts(
        lifts = this.workouts.flatMap { workout ->
            workout.lifts
        },
        impact = impact,
    )
}

/**
 * Creates a copy of a `LoggingSetUiModel` with potentially modified properties.
 *
 * This function acts as a generic copier for different subtypes of `LoggingSetUiModel`
 * (e.g., `LoggingStandardSetUiModel`, `LoggingMyoRepSetUiModel`, `LoggingDropSetUiModel`).
 * It allows overriding common properties shared across these types. If a property is not
 * provided, the original value from `this` set is used.
 *
 * @param position The new position of the set.
 * @param myoRepSetPosition The new position within a MyoRepSet (if applicable).
 * @param rpeTarget The new target RPE.
 * @param repRangeBottom The new bottom of the rep range.
 * @param repRangeTop The new top of the rep range.
 * @param weightRecommendation The new recommended weight.
 * @param hadInitialWeightRecommendation Whether the set initially had a weight recommendation.
 * @param previousSetResultLabel The label for the previous set's result.
 * @param repRangePlaceholder The placeholder text for the rep range.
 * @param setNumberLabel The label for the set number.
 * @param completedWeight The weight completed for the set.
 * @param completedReps The reps completed for the set.
 * @param completedRpe The RPE achieved for the set.
 * @param complete Whether the set is marked as complete.
 * @return A new `LoggingSetUiModel` instance of the same subtype as `this`, but with
 *         the specified properties updated.
 * @throws Exception if `this` is an unknown subtype of `LoggingSetUiModel`.
 *
 * Example Usage: `val updatedSet = existingSet.copyGeneric(completedReps = 10, complete = true)`
 */
fun LoggingSetUiModel.copyGeneric(
    position: Int = this.position,
    myoRepSetPosition: Int? = (this as? LoggingMyoRepSetUiModel)?.myoRepSetPosition,
    rpeTarget: Float = this.rpeTarget,
    repRangeBottom: Int? = this.repRangeBottom,
    repRangeTop: Int? = this.repRangeTop,
    weightRecommendation: Float? = this.weightRecommendation,
    hadInitialWeightRecommendation: Boolean = this.hadInitialWeightRecommendation,
    previousSetResultLabel: String = this.previousSetResultLabel,
    repRangePlaceholder: String = this.repRangePlaceholder,
    setNumberLabel: String = this.setNumberLabel,
    completedWeight: Float? = this.completedWeight,
    completedReps: Int? = this.completedReps,
    completedRpe: Float? = this.completedRpe,
    complete: Boolean = this.complete
): LoggingSetUiModel = when(this) {
    is LoggingStandardSetUiModel -> this.copy(
        position = position,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom!!,
        repRangeTop = repRangeTop!!,
        weightRecommendation = weightRecommendation,
        hadInitialWeightRecommendation = hadInitialWeightRecommendation,
        previousSetResultLabel = previousSetResultLabel,
        repRangePlaceholder = repRangePlaceholder,
        setNumberLabel = setNumberLabel,
        completedWeight = completedWeight,
        completedReps = completedReps,
        completedRpe = completedRpe,
        complete = complete
    )
    is LoggingMyoRepSetUiModel -> this.copy(
        position = position,
        myoRepSetPosition = myoRepSetPosition,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom,
        repRangeTop = repRangeTop,
        weightRecommendation = weightRecommendation,
        hadInitialWeightRecommendation = hadInitialWeightRecommendation,
        previousSetResultLabel = previousSetResultLabel,
        repRangePlaceholder = repRangePlaceholder,
        setNumberLabel = setNumberLabel,
        completedWeight = completedWeight,
        completedReps = completedReps,
        completedRpe = completedRpe,
        complete = complete
    )
    is LoggingDropSetUiModel -> this.copy(
        position = position,
        rpeTarget = rpeTarget,
        repRangeBottom = repRangeBottom!!,
        repRangeTop = repRangeTop!!,
        weightRecommendation = weightRecommendation,
        hadInitialWeightRecommendation = hadInitialWeightRecommendation,
        previousSetResultLabel = previousSetResultLabel,
        repRangePlaceholder = repRangePlaceholder,
        setNumberLabel = setNumberLabel,
        completedWeight = completedWeight,
        completedReps = completedReps,
        completedRpe = completedRpe,
        complete = complete
    )
    else -> throw Exception("${this::class.simpleName} is not defined.")
}

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
fun WorkoutUiModel.getRecalculatedWorkoutLiftStepSizeOptions(
    programDeloadWeek: Int,
    liftLevelDeloadsEnabled: Boolean,
): Map<Long, Map<Int, List<Int>>> {
    return lifts
        .filterIsInstance<StandardWorkoutLiftUiModel>()
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
fun StandardWorkoutLiftUiModel.getRecalculatedStepSizeForLift(
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
fun CustomLiftSetUiModel.transformToType(
    newSetType: SetType,
    defaultDropPercentage: Float = 0.1f,
    defaultMyoRepFloor: Int = 5,
    defaultMyoSetGoal: Int = 3,
    defaultStandardRpe: Float = 8.0f
): CustomLiftSetUiModel {
    // If we are transforming to the same type, just return the original object.
    val currentType = when (this) {
        is StandardSetUiModel -> SetType.STANDARD
        is DropSetUiModel -> SetType.DROP_SET
        is MyoRepSetUiModel -> SetType.MYOREP
        else -> null // Handle unknown types
    }
    if (newSetType == currentType) return this

    return when (this) {
        is StandardSetUiModel ->
            when (newSetType) {
                SetType.DROP_SET -> DropSetUiModel(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    dropPercentage = defaultDropPercentage,
                    rpeTarget = this.rpeTarget,
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
                SetType.MYOREP -> MyoRepSetUiModel(
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
        is MyoRepSetUiModel ->
            when (newSetType) {
                SetType.DROP_SET -> DropSetUiModel(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    dropPercentage = defaultDropPercentage,
                    rpeTarget = this.rpeTarget, // Prefer inheriting RPE
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
                SetType.STANDARD -> StandardSetUiModel(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    rpeTarget = defaultStandardRpe,
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                )
                SetType.MYOREP -> this // Already handled above
            }
        is DropSetUiModel ->
            when (newSetType) {
                SetType.MYOREP -> MyoRepSetUiModel(
                    id = this.id,
                    workoutLiftId = this.workoutLiftId,
                    position = this.position,
                    repFloor = defaultMyoRepFloor,
                    repRangeBottom = this.repRangeBottom,
                    repRangeTop = this.repRangeTop,
                    rpeTarget = this.rpeTarget,
                    setGoal = defaultMyoSetGoal,
                )
                SetType.STANDARD -> StandardSetUiModel(
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
