package com.browntowndev.liftlab.core.domain.extensions

import androidx.compose.ui.util.fastFlatMap
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.utils.generateFirstCompleteStepSequence
import com.browntowndev.liftlab.core.domain.utils.getPossibleStepSizes
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

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
 * @return A map where the key is the `workoutId` (Long) and the value is the
 *   `StandardWorkoutLift` with the recalculated step size. Only lifts with changed step sizes are included.
 */
fun List<Workout>.getAllLiftsWithRecalculatedStepSize(deloadToUseInsteadOfLiftLevel: Int?): Map<Long, List<StandardWorkoutLift>> {
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
                    workoutLift.workoutId to workoutLift.copy(stepSize = newStepSize)
                } else null
            }
        }.groupBy({ it.first }, { it.second })
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
 *
 * @return A new [CustomWorkoutLift] instance.
 */
fun StandardWorkoutLift.generateCustomSets(): List<GenericLiftSet> {
    val customSets = mutableListOf<GenericLiftSet>()
    for (i in 0 until setCount) {
        customSets.add(
            StandardSet(
                workoutLiftId = id,
                position = i,
                rpeTarget = getRpeTarget(
                    setIndex = i,
                    setCount = setCount,
                    progressionScheme = progressionScheme,
                    topSetRpeTarget = rpeTarget
                ),
                repRangeBottom = repRangeBottom,
                repRangeTop = repRangeTop
            )
        )
    }

    return customSets
}

/**
 * Build all RPE targets for a lift, given:
 * - top set RPE (first set),
 * - total number of sets including the final AMRAP (which is fixed at RPE 10),
 * - rounding step (default 0.5),
 * - and a maximum pre-final RPE cap (default 9.5).
 *
 * Indexing & counting:
 * - Set indices are 0-based: 0 == top set, (setCount - 1) == final set.
 * - setCount is the total number of sets (includes the final AMRAP).
 *
 * Progression design (no hardcoding of scenarios):
 * - We treat the jump from the top set to 10 RPE as `numberOfIncrements = setCount - 1`.
 * - We choose a target for the penultimate set (usually 9.0; allow 9.5 when small equal steps are natural).
 * - We find a geometric ratio r (via binary search) so that the cumulative share of the total RPE gap
 *   allocated by the first (k-1) increments lands at that penultimate target.
 * - Rounds intermediate sets to the nearest `roundingStep` and caps them at `maxPreFinalRpe`.
 */
fun getStraightSetsRpeTarget(
    topSetRpeTarget: Float,
    setCount: Int,
    roundingStep: Double = 0.5,
    maxPreFinalRpe: Double = 9.5
): List<Float> {
    if (setCount == 1) return listOf(topSetRpeTarget)

    val numberOfIncrements = setCount - 1                       // steps from top → final(10)
    val topRpeAsDouble = topSetRpeTarget.toDouble()
    val totalRpeGapToTen = (10.0 - topRpeAsDouble).coerceAtLeast(0.0)

    // Trivial case: only two sets (top + final). No intermediate to compute.
    if (numberOfIncrements == 1) {
        return listOf(topSetRpeTarget, 10f)
    }

    // Helper rounding utilities
    fun roundToNearestStep(value: Double, step: Double): Double =
        round(value / step) * step

    fun ceilToNearestStep(value: Double, step: Double): Double =
        ceil(value / step) * step

    // If top set is already at 10 (should be unusual), just return 10s.
    if (totalRpeGapToTen == 0.0) {
        return List(setCount) { 10f }
    }

    // Average increment if we split the total gap linearly.
    val averageIncrementIfLinear = totalRpeGapToTen / numberOfIncrements

    // Pick a principled penultimate target:
    // - Prefer 9.0 for most cases (gives room for a final push).
    // - If equal, tiny steps are natural (<= roundingStep), allow 9.5.
    //   Generalized using ceil to the configured rounding step.
    val penultimateCandidateDrop = ceilToNearestStep(averageIncrementIfLinear, roundingStep)
    val penultimateSetTarget = max(9.0, 10.0 - penultimateCandidateDrop)

    // The fraction of the total gap we want consumed by the first (k-1) increments.
    val desiredPenultimateFraction =
        ((penultimateSetTarget - topRpeAsDouble) / totalRpeGapToTen).coerceIn(0.0, 1.0)

    // For a geometric series with ratio r and k increments, the fraction of the
    // total k-weight summed by the first (k-1) increments is:
    //   f(r, k) = (1 - r^(k-1)) / (1 - r^k),   with the r→1 limit = (k-1)/k
    fun fractionOfGapAtPenultimate(geometricRatio: Double, increments: Int): Double {
        if (increments <= 1) return 0.0
        return if (abs(geometricRatio - 1.0) < 1e-9) {
            (increments - 1).toDouble() / increments.toDouble()
        } else {
            val rPowKm1 = geometricRatio.pow(increments - 1)
            val rPowK = rPowKm1 * geometricRatio
            (1 - rPowKm1) / (1 - rPowK)
        }
    }

    val fractionIfEqualSteps = (numberOfIncrements - 1).toDouble() / numberOfIncrements.toDouble()

    // Solve for r (strictly decreasing mapping → binary search is fine).
    val geometricRatio = if (abs(desiredPenultimateFraction - fractionIfEqualSteps) < 1e-6) {
        1.0 // linear steps
    } else {
        var lowRatio = 1e-6
        var highRatio = 100.0
        repeat(60) {
            val mid = (lowRatio + highRatio) / 2.0
            val fractionAtMid = fractionOfGapAtPenultimate(mid, numberOfIncrements)
            if (fractionAtMid > desiredPenultimateFraction) {
                // Need to push more of the gap into later sets → increase r
                lowRatio = mid
            } else {
                highRatio = mid
            }
        }
        (lowRatio + highRatio) / 2.0
    }

    // Sum of the first N terms of a geometric series with ratio r.
    fun geometricSeriesSum(ratio: Double, terms: Int): Double =
        if (abs(ratio - 1.0) < 1e-9) {
            terms.toDouble()
        } else {
            (1 - ratio.pow(terms)) / (1 - ratio)
        }

    // Base increment so that all geometric increments sum to totalRpeGapToTen.
    val totalGeometricWeights = geometricSeriesSum(geometricRatio, numberOfIncrements)
    val baseIncrementSize = totalRpeGapToTen / totalGeometricWeights

    // Cumulative increase after i increments (i == setIndex for intermediate sets).
    fun cumulativeIncreaseAfter(incrementsCompleted: Int): Double {
        return if (abs(geometricRatio - 1.0) < 1e-9) {
            baseIncrementSize * incrementsCompleted
        } else {
            baseIncrementSize * (1 - geometricRatio.pow(incrementsCompleted)) / (1 - geometricRatio)
        }
    }

    // Build the full sequence once (index 0 through setCount-1).
    return (0 until setCount).map { setIndex ->
        when (setIndex) {
            0 -> topRpeAsDouble
            setCount - 1 -> 10.0
            else -> {
                val rawTarget = topRpeAsDouble + cumulativeIncreaseAfter(setIndex)
                val roundedTarget = roundToNearestStep(rawTarget, roundingStep)
                roundedTarget.coerceAtMost(maxPreFinalRpe)
            }
        }.toFloat()
    }
}

/**
 * Calculates the RPE target based on the current set index, set count, progression scheme, and top set RPE target.
 *
 * @param setIndex The index of the current set.
 * @param setCount The total number of sets in the lift.
 * @param progressionScheme The progression scheme of the lift.
 * @param topSetRpeTarget The RPE target of the top set.
 * @return The calculated RPE target for custom lift sets.
 */
fun getRpeTarget(setIndex: Int, setCount: Int, progressionScheme: ProgressionScheme, topSetRpeTarget: Float) =
    when (progressionScheme) {
        ProgressionScheme.DOUBLE_PROGRESSION, ProgressionScheme.WAVE_LOADING_PROGRESSION -> {
            val rpeSeries = getStraightSetsRpeTarget(
                setCount = setCount,
                topSetRpeTarget = topSetRpeTarget
            )
            val safeIndex = setIndex.coerceIn(0, setCount - 1)
            rpeSeries[safeIndex]
        }
        ProgressionScheme.TOP_SET_PROGRESSION -> 10f
        else -> topSetRpeTarget
    }
