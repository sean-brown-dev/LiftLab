package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

class DoubleProgressionCalculator: StraightSetProgressionCalculator() {

    override fun allSetsMetCriterion(
        lift: StandardWorkoutLiftDto,
        previousSetResults: List<SetResult>,
    ): Boolean {
        val allSetGoalsMet = previousSetResults.all {
            it.reps >= lift.repRangeTop && it.rpe <= lift.rpeTarget
        }

        return allSetGoalsMet
    }

    override fun allSetsMetCriterion(
        lift: CustomWorkoutLiftDto,
        previousSetResults: List<SetResult>,
    ): Boolean {
        val groupedSetData = previousSetResults.groupBy { it.setPosition }
        val allSetsMetCriterion =
            groupedSetData.size == lift.setCount &&
                    lift.customLiftSets.all { set ->
                        when (set) {
                            is MyoRepSetDto -> {
                                customSetMeetsCriterion(
                                    set = set,
                                    setData = groupedSetData[set.setPosition]?.filterIsInstance<MyoRepSetResultDto>()
                                )
                            }
                            else -> {
                                customSetMeetsCriterion(
                                    set = set,
                                    previousSet = groupedSetData[set.setPosition]?.firstOrNull()
                                )
                            }
                        }
                    }

        return allSetsMetCriterion
    }
}