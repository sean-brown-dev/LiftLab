package com.browntowndev.liftlab.core.domain.progression

import com.browntowndev.liftlab.core.domain.models.LoggingMyoRepSet

class MyoRepSetGoalValidator {
    companion object {
        fun shouldContinueMyoReps(
            completedSet: LoggingMyoRepSet,
            myoRepSetResults: List<LoggingMyoRepSet>,
            activationSetAlwaysSuccess: Boolean = false,
        ): Boolean {
            return myoRepSetResults.last() == completedSet &&
                    allSetsCompleted(myoRepSetResults = myoRepSetResults) &&
                    shouldContinueMyoReps(myoRepSetResults = myoRepSetResults, activationSetAlwaysSuccess = activationSetAlwaysSuccess)
        }

        private fun allSetsCompleted(myoRepSetResults: List<LoggingMyoRepSet>): Boolean {
            return myoRepSetResults.all {
                it.complete &&
                        it.completedReps != null &&
                        it.completedWeight != null &&
                        it.completedRpe != null
            }
        }

        private fun shouldContinueMyoReps(
            myoRepSetResults: List<LoggingMyoRepSet>,
            activationSetAlwaysSuccess: Boolean,
        ): Boolean {
            val completedSet = myoRepSetResults.last()
            val isActivationSet = myoRepSetResults.size == 1

            return if (isActivationSet) {
                activationSetAlwaysSuccess || (completedSet.completedReps!! + (10 - completedSet.completedRpe!!)) >=
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
        }
    }
}