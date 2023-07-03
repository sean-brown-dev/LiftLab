package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.persistence.dtos.ProgressionDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

class WaveLoadingProgressionCalculator(private val programDeloadWeek: Int): BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
    ): List<ProgressionDto> {
        val sortedSetData = previousSetResults.sortedBy { it.setPosition }
        val deloadWeek = (workoutLift.deloadWeek ?: programDeloadWeek) - 1 // deloadWeek starts at 1 so subtract 1

        return sortedSetData.map {
            ProgressionDto(
                setPosition = it.setPosition,
                weightRecommendation =
                    if (deloadWeek != (it.microCycle + 1))
                        incrementWeight(workoutLift, it)
                    else
                        decrementForDeload(lift = workoutLift, setData = it, deloadWeek = deloadWeek)
            )
        }
    }

    private fun decrementForDeload(
        lift: GenericWorkoutLift,
        setData: SetResult,
        deloadWeek: Int
    ): Float {
        val increment =  (lift.incrementOverride ?: lift.liftIncrementOverride
        ?: SettingsManager.getSetting(SettingsManager.SettingNames.INCREMENT_AMOUNT, 5f)).toInt()

        return setData.weight - (increment * (deloadWeek - 1))
    }
}