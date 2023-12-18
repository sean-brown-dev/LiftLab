package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto

class MyoRepSetGoalValidator {
    companion object {
        fun shouldContinueMyoReps(
            completedRpe: Float,
            completedReps: Int,
            myoRepSetGoals: LoggingMyoRepSetDto,
            previousMyoRepSetResults: List<LoggingMyoRepSetDto>,
            ): Boolean {
            val isActivationSet = previousMyoRepSetResults.isEmpty()
            val metGoals = completedRpe <= myoRepSetGoals.rpeTarget

            return metGoals && if (isActivationSet) {
                completedReps >= myoRepSetGoals.repRangeBottom!!
            } else if (myoRepSetGoals.setMatching) {
                val totalRepsCompleted = previousMyoRepSetResults.sumOf { prevSet ->
                    if (prevSet.myoRepSetPosition != null) {
                        prevSet.completedReps ?: 0
                    } else 0
                } + completedReps

                previousMyoRepSetResults
                    .find { it.myoRepSetPosition == null }!!
                    .let { activationSet -> activationSet.repRangeTop!! > totalRepsCompleted }
            } else {
                previousMyoRepSetResults.size + 1 < (myoRepSetGoals.maxSets ?: Int.MAX_VALUE) &&
                        completedReps > myoRepSetGoals.repFloor!!
            }
        }

        fun shouldContinueMyoReps(
            completedSet: LoggingMyoRepSetDto,
            myoRepSetResults: List<LoggingMyoRepSetDto>,
        ): Boolean {
            if (!completedSet.complete ||
                completedSet.completedReps == null ||
                completedSet.completedWeight == null ||
                completedSet.completedRpe == null ||
                myoRepSetResults.any {
                    (completedSet.myoRepSetPosition ?: -1) < (it.myoRepSetPosition ?: -1)
                }) {
                return false
            }

            return shouldContinueMyoReps(
                completedRpe = completedSet.completedRpe,
                completedReps = completedSet.completedReps,
                myoRepSetGoals = completedSet,
                previousMyoRepSetResults = if (completedSet.myoRepSetPosition != null)
                    myoRepSetResults.filter { it.myoRepSetPosition != completedSet.myoRepSetPosition }
                else listOf()
            )
        }
    }
}