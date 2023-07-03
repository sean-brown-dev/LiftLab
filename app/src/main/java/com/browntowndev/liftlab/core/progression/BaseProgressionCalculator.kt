package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

abstract class BaseProgressionCalculator: ProgressionCalculator {
    protected fun incrementWeight(lift: GenericWorkoutLift, prevSet: SetResult): Float {
        return prevSet.weight + (lift.incrementOverride ?: lift.liftIncrementOverride
        ?: SettingsManager.getSetting(SettingsManager.SettingNames.INCREMENT_AMOUNT, 5f)).toInt()
    }

    protected fun customSetMeetsCriterion(set: GenericCustomLiftSet, previousSet: SetResult?): Boolean {
        return previousSet != null && set.rpeTarget == previousSet.rpe && set.repRangeTop <= previousSet.reps
    }

    protected fun customSetMeetsCriterion(
        set: MyoRepSetDto,
        setData: List<MyoRepSetResultDto>?,
    ) : Boolean {
        if (setData == null) return false

        val activationSet = setData.first()
        val myoRepSets = setData.filter { it.myoRepSetPosition != null }
        val criterionMet = set.repRangeTop <= activationSet.reps &&
                set.rpeTarget >= activationSet.rpe &&
                (myoRepSets.all { set.rpeTarget >= it.rpe })

        return criterionMet && if (set.setMatching) {
            set.setGoal >= myoRepSets.size && myoRepSets.sumOf { it.reps } >= set.repRangeTop
        } else {
            set.setGoal <= myoRepSets.size
        }
    }
}