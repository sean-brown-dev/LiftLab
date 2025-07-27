package com.browntowndev.liftlab.core.domain.useCase.utils

import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet

class MyoRepSetGoalUtils {
    companion object {
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

            val success = if (isActivationSet) {
                (completedSet.completedReps!! + (10 - completedSet.completedRpe!!)) >=
                        (completedSet.repRangeBottom!! + (10 - completedSet.rpeTarget))
            } else if (completedSet.setMatching) {
                val myoRepSetsExcludingActivation = myoRepSetResults.filter { it.myoRepSetPosition != null }
                val totalRepsCompleted = myoRepSetsExcludingActivation.sumOf { it.completedReps ?: 0 }

                myoRepSetResults
                    .find { it.myoRepSetPosition == null }!!
                    .let { activationSet -> activationSet.completedReps!! > totalRepsCompleted }
            } else {
                myoRepSetResults.size < (completedSet.maxSets ?: Int.MAX_VALUE) &&
                        completedSet.completedReps!! > completedSet.repFloor!!
            }

            return MyoRepContinuationResult(
                shouldContinueMyoReps = isActivationSet || success,
                activationSetMissedGoal = isActivationSet && !success,
            )
        }
    }
}