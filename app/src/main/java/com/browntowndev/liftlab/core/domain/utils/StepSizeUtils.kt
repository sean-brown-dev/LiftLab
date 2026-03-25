package com.browntowndev.liftlab.core.domain.utils

/**
 * Calculates a list of possible step sizes for a wave-loading progression.
 *
 * A step size is considered possible if it evenly divides the difference between
 * the `repRangeTop` and `repRangeBottom`.
 *
 * Additionally, if `stepCount` is provided (typically derived from the number of weeks
 * in a training cycle excluding deload), the function ensures that the number of
 * steps required to reach the `repRangeBottom` from `repRangeTop` (plus one for the
 * initial step at `repRangeTop`) is a divisor of `stepCount + 1`. This ensures the
 * rep sequence will align correctly within the cycle.
 *
 * @param repRangeTop The highest number of reps in the range.
 * @param repRangeBottom The lowest number of reps in the range.
 * @param stepCount Optional. The number of progression steps available in the cycle (e.g., weeks - 2 for deload).
 * @return A list of valid step sizes (List<Int>).
 */
fun getPossibleStepSizes(
    repRangeTop: Int,
    repRangeBottom: Int,
    stepCount: Int?
): List<Int> {
    val rangeSize = repRangeTop - repRangeBottom
    val stepSizes = mutableListOf<Int>()

    // Calculate possible step sizes
    for (i in 1..rangeSize) {
        val canBeReachedInSteps =
            stepCount == null || (stepCount + 1) % ((rangeSize / i) + 1) == 0
        if (rangeSize % i == 0 && canBeReachedInSteps) {
            stepSizes.add(i)
        }
    }

    return stepSizes
}

/**
 * Generates the first complete sequence of reps for a wave-loading progression.
 *
 * This sequence starts at `repRangeTop` and decreases by `stepSize` until
 * `repRangeBottom` is reached.
 *
 * @param repRangeTop The highest number of reps in the range.
 * @param repRangeBottom The lowest number of reps in the range.
 * @param stepSize The amount by which reps decrease in each step.
 * @return A list of integers representing the rep sequence (e.g., [10, 8, 6]).
 */
fun generateFirstCompleteStepSequence(
    repRangeTop: Int,
    repRangeBottom: Int,
    stepSize: Int
): List<Int> {
    val steps = mutableListOf<Int>()
    val stepsToRepRangeBottom = (repRangeTop - repRangeBottom) / stepSize

    for (i in 0..stepsToRepRangeBottom) {
        val currStepSizeFromTop = i * stepSize
        steps.add(repRangeTop - currStepSizeFromTop)
    }

    return steps
}

/**
 * Generates a complete sequence of reps for a wave-loading progression over a specified number of total steps.
 *
 * This function first calculates the basic repeating sequence of reps from `repRangeTop`
 * down to `repRangeBottom` using the given `stepSize`.
 *
 * Then, it repeats this basic sequence as many times as needed to fill the `totalStepsToTake`.
 * If `totalStepsToTake` is not a multiple of the basic sequence length, the sequence
 * will be truncated or repeated partially to match the `totalStepsToTake`.
 *
 * @param repRangeTop The highest number of reps in the range.
 * @param repRangeBottom The lowest number of reps in the range.
 * @param stepSize The amount by which reps decrease in each step of the basic sequence.
 * @param totalStepsToTake The total number of steps (e.g., workouts in a cycle) for which to generate reps.
 * @return A list of integers representing the rep sequence for the entire duration.
 */
fun generateCompleteStepSequence(
    repRangeTop: Int,
    repRangeBottom: Int,
    stepSize: Int,
    totalStepsToTake: Int
): List<Int> {
    val steps = mutableListOf<Int>()
    val stepsToRepRangeBottom = (repRangeTop - repRangeBottom) / stepSize

    for (i in 0..stepsToRepRangeBottom) {
        val currStepSizeFromTop = i * stepSize
        steps.add(repRangeTop - currStepSizeFromTop)
    }

    return List(size = totalStepsToTake) { steps[it % steps.size] }
}