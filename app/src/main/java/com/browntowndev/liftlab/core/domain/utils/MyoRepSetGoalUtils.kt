package com.browntowndev.liftlab.core.domain.utils

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.roundToOneDecimal
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult

class MyoRepSetGoalUtils {
    companion object {
        fun resultsMetGoals(
            set: CalculationMyoRepSet,
            setData: List<MyoRepSetResult>?
        ): Boolean {
            if (setData.isNullOrEmpty()) return false

            val activationSet = setData.first()
            val activationResult = validateActivationSetGoals(
                completedReps = activationSet.reps,
                completedRpe = activationSet.rpe,
                repTarget = set.repRangeTop,
                rpeTarget = set.rpeTarget)
            if (setData.size == 1 || !activationResult) return activationResult

            // Validate set matching goals, if enabled
            val setMatchingGoalsMet = !set.setMatching ||
                    validateSetMatchingGoals(myoRepSetResults = setData.fastMap { result ->
                        MyoRepCompletedReps(
                            myoRepSetPosition = result.myoRepSetPosition,
                            completedReps = result.reps,
                        )
                    })
            if (!setMatchingGoalsMet) return false

            val maxSetsAndFloorGoalsMet = set.setMatching ||
                    validateMaxSetsAndSetFloorGoals(
                        setsCompleted = setData.size,
                        maxSets = set.maxSets ?: Int.MAX_VALUE,
                        completedReps = setData.sumOf { it.reps },
                        repFloor = set.repFloor ?: 5,
                    )

            return maxSetsAndFloorGoalsMet
        }

        fun shouldContinueMyoReps(
            lastMyoRepSet: LoggingMyoRepSet,
            myoRepSetResults: List<LoggingMyoRepSet>,
        ): MyoRepContinuationResult {
            val basicChecksPassed = lastMyoRepSet.complete &&
                    myoRepSetResults.last() == lastMyoRepSet &&
                    allSetsCompleted(myoRepSetResults = myoRepSetResults)

            return if (basicChecksPassed) getContinuationResultByCheckingSetGoals(completedSet = lastMyoRepSet, myoRepSetResults = myoRepSetResults)
            else return MyoRepContinuationResult(
                shouldContinueMyoReps = false,
                activationSetMissedGoal = false,
            )
        }

        private fun allSetsCompleted(myoRepSetResults: List<LoggingMyoRepSet>): Boolean {
            return myoRepSetResults.all {
                it.complete &&
                        it.completedReps != null &&
                        it.completedWeight != null &&
                        it.completedRpe != null
            }
        }

        private fun getContinuationResultByCheckingSetGoals(
            completedSet: LoggingMyoRepSet,
            myoRepSetResults: List<LoggingMyoRepSet>
        ): MyoRepContinuationResult {
            val isActivationSet = myoRepSetResults.size == 1

            val myoRepSetGoalAndCompletionData = MyoRepSetGoalAndCompletionData(
                completedReps = completedSet.completedReps!!,
                completedRpe = completedSet.completedRpe!!,
                repRangeBottom = completedSet.repRangeBottom!!,
                rpeTarget = completedSet.rpeTarget,
                setMatching = completedSet.setMatching,
                maxSets = completedSet.maxSets ?: Int.MAX_VALUE,
                repFloor = completedSet.repFloor ?: 5,
            )

            return determineContinuationResult(
                isActivationSet = isActivationSet,
                myoRepSetGoalAndCompletionData = myoRepSetGoalAndCompletionData,
                myoRepSetResults = myoRepSetResults.fastMap { result ->
                    MyoRepCompletedReps(
                        myoRepSetPosition = result.myoRepSetPosition,
                        completedReps = result.completedReps,
                    )
                }
            )
        }

        private fun determineContinuationResult(
            isActivationSet: Boolean,
            myoRepSetGoalAndCompletionData: MyoRepSetGoalAndCompletionData,
            myoRepSetResults: List<MyoRepCompletedReps>
        ): MyoRepContinuationResult {
            val success = when {
                isActivationSet -> validateActivationSetGoals(
                    completedReps = myoRepSetGoalAndCompletionData.completedReps,
                    completedRpe = myoRepSetGoalAndCompletionData.completedRpe,
                    repTarget = myoRepSetGoalAndCompletionData.repRangeBottom,
                    rpeTarget = myoRepSetGoalAndCompletionData.rpeTarget,
                )
                myoRepSetGoalAndCompletionData.setMatching -> !validateSetMatchingGoals(myoRepSetResults = myoRepSetResults)
                else -> validateMaxSetsAndSetFloorGoals(
                    setsCompleted = myoRepSetResults.size,
                    maxSets = myoRepSetGoalAndCompletionData.maxSets,
                    completedReps = myoRepSetGoalAndCompletionData.completedReps,
                    repFloor = myoRepSetGoalAndCompletionData.repFloor,
                )
            }

            return MyoRepContinuationResult(shouldContinueMyoReps = isActivationSet || success, activationSetMissedGoal = isActivationSet && !success)
        }

        private fun validateActivationSetGoals(
            completedReps: Int,
            completedRpe: Float,
            repTarget: Int,
            rpeTarget: Float,
        ): Boolean = (completedReps + (10f - completedRpe)).roundToOneDecimal() >=
                (repTarget + (10 - rpeTarget)).roundToOneDecimal()

        private fun validateSetMatchingGoals(
            myoRepSetResults: List<MyoRepCompletedReps>,
        ): Boolean {
            val miniSets = myoRepSetResults.filter { it.myoRepSetPosition != null }
            val totalRepsCompleted = miniSets.sumOf { it.completedReps ?: 0 }

            return myoRepSetResults
                .find { it.myoRepSetPosition == null }
                ?.let { activationSet -> (activationSet.completedReps ?: 0) <= totalRepsCompleted } ?: false
        }

        private fun validateMaxSetsAndSetFloorGoals(
            setsCompleted: Int,
            maxSets: Int,
            completedReps: Int,
            repFloor: Int,
        ): Boolean = setsCompleted < maxSets && completedReps > repFloor
    }
}

private data class MyoRepCompletedReps(
    val myoRepSetPosition: Int?,
    val completedReps: Int?,
)

private data class MyoRepSetGoalAndCompletionData(
    /**
     * The number of reps completed for the set. Used to see if rep floor was breached.
     */
    val completedReps: Int,

    /**
     * The RPE completed for the set. Only used if it is the activation set.
     */
    val completedRpe: Float,

    /**
     * The bottom of the rep range for the set. Only used if it is the activation set.
     */
    val repRangeBottom: Int,

    /**
     * The RPE target for the set. Only used if it is the activation set.
     */
    val rpeTarget: Float,
    val setMatching: Boolean,
    val maxSets: Int,
    val repFloor: Int
)