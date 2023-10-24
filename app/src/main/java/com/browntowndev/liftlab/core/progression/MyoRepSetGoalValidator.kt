package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto

class MyoRepSetGoalValidator {
    companion object {
        fun shouldContinueMyoReps(
                completedRpe: Float,
                completedReps: Int,
                myoRepSetGoals: LoggingMyoRepSetDto,
                previousMyoRepSets: List<LoggingMyoRepSetDto>,
            ): Boolean {
            val isActivationSet = previousMyoRepSets.isEmpty()
            val metGoals = completedRpe == myoRepSetGoals.rpeTarget

            return metGoals && if (isActivationSet) {
                completedReps >= myoRepSetGoals.repRangeBottom
            } else if (myoRepSetGoals.setMatching) {
                val totalSetsCompleted = previousMyoRepSets.sumOf { prevSet ->
                    if (prevSet.myoRepSetPosition != null) {
                        prevSet.completedReps ?: 0
                    } else 0
                } + completedReps

                myoRepSetGoals.repRangeTop > totalSetsCompleted
            } else {
                previousMyoRepSets.size + 1 < (myoRepSetGoals.maxSets ?: Int.MAX_VALUE) &&
                        completedReps > myoRepSetGoals.repFloor!!
            }
        }

        fun shouldContinueMyoReps(
            completedSet: LoggingMyoRepSetDto,
            previousMyoRepSets: List<LoggingMyoRepSetDto>,
        ): Boolean {
            if (!completedSet.complete ||
                completedSet.completedReps == null ||
                completedSet.completedWeight == null ||
                completedSet.completedRpe == null) {
                return false
            }

            return shouldContinueMyoReps(
                completedRpe = completedSet.completedRpe,
                completedReps = completedSet.completedReps,
                myoRepSetGoals = completedSet,
                previousMyoRepSets = previousMyoRepSets)
        }
    }
}