package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

class WaveLoadingProgressionCalculator(private val programDeloadWeek: Int): BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        if (workoutLift !is StandardWorkoutLiftDto) throw Exception ("Wave Loading progression cannot have custom sets")
        val groupedSetData = previousSetResults.sortedBy { it.setPosition }.associateBy { it.setPosition }
        val deloadWeek = workoutLift.deloadWeek ?: programDeloadWeek

        return List(workoutLift.setCount) { setPosition ->
            val result = groupedSetData[setPosition]
            LoggingStandardSetDto(
                setPosition = setPosition,
                rpeTarget = workoutLift.rpeTarget,
                repRangeBottom = workoutLift.repRangeBottom,
                repRangeTop = workoutLift.repRangeTop,
                previousSetResultLabel =
                if (result != null) {
                    "${result.weight}x${result.reps} @${result.rpe}"
                } else "—",
                repRangePlaceholder = if (!isDeloadWeek) {
                    getRepRangePlaceholder(
                        repRangeBottom = workoutLift.repRangeBottom,
                        repRangeTop = workoutLift.repRangeTop,
                        microCycle = result?.microCycle ?: 0)
               } else {
                      workoutLift.repRangeBottom.toString()
                },
                weightRecommendation = if (!isDeloadWeek && result != null)
                    incrementWeight(workoutLift, result)
                else if (result != null)
                    decrementForDeload(lift = workoutLift, setData = result, deloadWeek = deloadWeek)
                else null
            )
        }
    }

    private fun getRepRangePlaceholder(repRangeBottom: Int, repRangeTop: Int, microCycle: Int): String {
        val fullRepRange = (repRangeBottom..repRangeTop).toList()
        val index = microCycle % fullRepRange.size
        return fullRepRange[index].toString()
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