package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto

class MyoRepSetGoalValidator {
    companion object {
        fun shouldContinueMyoReps(
                myoRepSetGoals: LoggingMyoRepSetDto,
                completedMyoRepSetResult: MyoRepSetResultDto,
                previousMyoRepSets: List<LoggingMyoRepSetDto>,
            ): Boolean {
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

                myoRepSetGoals.repRangeTop > totalSetsCompleted
            } else {
                previousMyoRepSets.size + 1 < (myoRepSetGoals.maxSets ?: Int.MAX_VALUE) &&
                        completedMyoRepSetResult.reps > myoRepSetGoals.repFloor!!
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

            val setResult = MyoRepSetResultDto(
                workoutId = 0L,
                liftId = 0L,
                mesoCycle = 0,
                microCycle = 0,
                setPosition = completedSet.setPosition,
                myoRepSetPosition = completedSet.myoRepSetPosition,
                weight = completedSet.completedWeight,
                reps = completedSet.completedReps,
                rpe = completedSet.completedRpe,
            )

            return shouldContinueMyoReps(
                myoRepSetGoals = completedSet,
                completedMyoRepSetResult = setResult,
                previousMyoRepSets = previousMyoRepSets)
        }
    }
}