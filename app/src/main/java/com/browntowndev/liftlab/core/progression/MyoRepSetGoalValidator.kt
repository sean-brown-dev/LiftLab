package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto

class MyoRepSetGoalValidator {
    companion object {
        fun validate(
                myoRepSetGoals: LoggingMyoRepSetDto,
                completedMyoRepSetResult: MyoRepSetResultDto,
                previousMyoRepSets: List<LoggingMyoRepSetDto>,
            ): Boolean {
                // TODO: Unit tests
            val isActivationSet = previousMyoRepSets.isEmpty()
            val metGoals = completedMyoRepSetResult.rpe == myoRepSetGoals.rpeTarget

            return metGoals && if (isActivationSet) {
                completedMyoRepSetResult.reps >= myoRepSetGoals.repRangeBottom
            } else if (myoRepSetGoals.setMatching) {
                val totalSetsCompleted = previousMyoRepSets.sumOf { prevSet ->
                    if (prevSet.myoRepSetPosition != null) {
                        prevSet.completedReps ?: 0
                    } else 0
                } + completedMyoRepSetResult.reps

                myoRepSetGoals.repRangeTop <= totalSetsCompleted
            } else {
                previousMyoRepSets.size + 1 < (myoRepSetGoals.maxSets ?: Int.MAX_VALUE) &&
                        completedMyoRepSetResult.reps > myoRepSetGoals.repFloor!!
            }
        }
    }
}