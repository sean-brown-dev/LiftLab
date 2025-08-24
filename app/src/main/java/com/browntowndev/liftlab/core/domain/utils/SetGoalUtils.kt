package com.browntowndev.liftlab.core.domain.utils

import com.browntowndev.liftlab.core.common.SET_TOO_EASY_REPS_THRESHOLD
import com.browntowndev.liftlab.core.common.SET_TOO_HARD_REPS_THRESHOLD
import com.browntowndev.liftlab.core.common.roundToOneDecimal

data class MissedGoalResult(
    val missedRepRangeBottom: Boolean,
    val exceededRepRangeTop: Boolean,
)

/**
 * Calculates whether the goal was exceeded, missed or neither.
 *
 * @param completedReps The completed reps.
 * @param completedRpe The completed RPE.
 * @param repRangeTop The rep range top.
 * @param repRangeBottom The rep range bottom.
 * @param rpeTarget The RPE target.
 * @param repRangeTopFatigueOffset The rep range top fatigue offset. A positive number reduces fatigue, a negative number increases it.
 * @param repRangeBottomFatigueOffset The rep range bottom fatigue offset. A positive number reduces fatigue, a negative number increases it.
 * @return The missed goal result.
 */
fun calculateMissedGoalResult(
    completedReps: Int,
    completedRpe: Float,
    repRangeTop: Int,
    repRangeBottom: Int,
    rpeTarget: Float,
    repRangeTopFatigueOffset: Float = SET_TOO_EASY_REPS_THRESHOLD,
    repRangeBottomFatigueOffset: Float = SET_TOO_HARD_REPS_THRESHOLD,
): MissedGoalResult {
    val rpeAdjustedRepsCompleted = getRpeAdjustedReps(completedReps, completedRpe)
    val rpeAdjustedRepRangeTop = getRpeAdjustedReps(repRangeTop, rpeTarget)
    val rpeAdjustedRepRangeBottom = getRpeAdjustedReps(repRangeBottom, rpeTarget)

    val exceededRepRangeTop = exceededRepRangeTop(rpeAdjustedRepRangeTop, rpeAdjustedRepsCompleted, repRangeTopFatigueOffset)
    val missedRepRangeBottom = missedRepRangeBottom(rpeAdjustedRepRangeBottom, rpeAdjustedRepsCompleted, repRangeBottomFatigueOffset)

    return MissedGoalResult(
        missedRepRangeBottom = missedRepRangeBottom,
        exceededRepRangeTop = exceededRepRangeTop,
    )
}

/**
 * Calculates whether the reps completed exceed the rep range top by more than `SET_TOO_EASY_REPS_THRESHOLD`.
 *
 * @param repRangeTop The rep range top.
 * @param rpeTarget The RPE target.
 * @param completedReps The completed reps.
 * @param completedRpe The completed RPE.
 * @param fatigueOffset The fatigue offset. A positive number reduces fatigue, a negative number increases it.
 * @return Whether the reps completed exceed the rep range top by more than `SET_TOO_EASY_REPS_THRESHOLD`.
 */
fun exceededRepRangeTop(
    repRangeTop: Int,
    rpeTarget: Float,
    completedReps: Int,
    completedRpe: Float,
    fatigueOffset: Float = SET_TOO_EASY_REPS_THRESHOLD,
): Boolean {
    val rpeAdjustedRepRangeTop = getRpeAdjustedReps(repRangeTop, rpeTarget)
    val rpeAdjustedRepsCompleted = getRpeAdjustedReps(completedReps, completedRpe)

    return exceededRepRangeTop(rpeAdjustedRepRangeTop, rpeAdjustedRepsCompleted, fatigueOffset)
}

/**
 * Calculates whether the reps completed missed the rep range bottom by more than `SET_TOO_HARD_REPS_THRESHOLD`.
 *
 * @param repRangeBottom The rep range bottom.
 * @param rpeTarget The RPE target.
 * @param completedReps The completed reps.
 * @param completedRpe The completed RPE.
 * @param fatigueOffset The fatigue offset. A positive number reduces fatigue, a negative number increases it.
 * @return Whether the reps completed missed the rep range bottom by more than `SET_TOO_HARD_REPS_THRESHOLD`.
 */
fun missedRepRangeBottom(
    repRangeBottom: Int,
    rpeTarget: Float,
    completedReps: Int,
    completedRpe: Float,
    fatigueOffset: Float = SET_TOO_HARD_REPS_THRESHOLD,
): Boolean {
    val rpeAdjustedRepRangeBottom = getRpeAdjustedReps(repRangeBottom, rpeTarget)
    val repsConsideringRpe = getRpeAdjustedReps(completedReps, completedRpe)

    return missedRepRangeBottom(rpeAdjustedRepRangeBottom, repsConsideringRpe, fatigueOffset)
}

private fun getRpeAdjustedReps(reps: Int, rpe: Float) =
    (reps + (10f - rpe)).roundToOneDecimal()

private fun exceededRepRangeTop(rpeAdjustedRepRangeTop: Float, rpeAdjustedRepsCompleted: Float, fatigueOffset: Float) =
    rpeAdjustedRepsCompleted >= (rpeAdjustedRepRangeTop + fatigueOffset).roundToOneDecimal()

private fun missedRepRangeBottom(rpeAdjustedRepRangeBottom: Float, rpeAdjustedRepsCompleted: Float, fatigueOffset: Float) =
    rpeAdjustedRepsCompleted <= (rpeAdjustedRepRangeBottom - fatigueOffset).roundToOneDecimal()
