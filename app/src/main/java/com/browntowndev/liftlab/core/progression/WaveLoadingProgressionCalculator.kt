package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

class WaveLoadingProgressionCalculator(
    private val programDeloadWeek: Int,
    private val microCycle: Int
): BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        if (workoutLift !is StandardWorkoutLiftDto) throw Exception ("Wave Loading progression cannot have custom sets")
        val groupedSetData = previousSetResults.sortedBy { it.setPosition }.associateBy { it.setPosition }
        val setCount = if (isDeloadWeek) 2 else workoutLift.setCount
        val rpeTarget = if (isDeloadWeek) 6f else workoutLift.rpeTarget

        return List(setCount) { setPosition ->
            val result = groupedSetData[setPosition]
            val weightRecommendation = if (!isDeloadWeek && result != null)
                getWeightRecommendation(workoutLift, result)
            else if (result != null)
                decrementForDeload(lift = workoutLift, setData = result, deloadWeek = workoutLift.deloadWeek ?: programDeloadWeek)
            else null

            LoggingStandardSetDto(
                position = setPosition,
                rpeTarget = rpeTarget,
                repRangeBottom = workoutLift.repRangeBottom,
                repRangeTop = workoutLift.repRangeTop,
                previousSetResultLabel = getPreviousSetResultLabel(result),
                repRangePlaceholder = if (!isDeloadWeek) {
                    getRepRangePlaceholder(
                        repRangeBottom = workoutLift.repRangeBottom,
                        repRangeTop = workoutLift.repRangeTop,
                        microCycle = microCycle)
               } else {
                      workoutLift.repRangeBottom.toString()
                },
                weightRecommendation = weightRecommendation,
                hadInitialWeightRecommendation = weightRecommendation != null,
            )
        }.flattenWeightRecommendationsStandard()
    }

    private fun getWeightRecommendation(workoutLift: GenericWorkoutLift, result: SetResult): Float {
        return if (microCycle == 0 ||
            !shouldDecreaseWeight(result, workoutLift as StandardWorkoutLiftDto)
        ) {
            incrementWeight(workoutLift, result)
        } else {
            getRepsForPreviousWorkout(
                repRangeBottom = workoutLift.repRangeBottom,
                repRangeTop = workoutLift.repRangeTop,
                microCycle = microCycle,
            )?.let { reps ->
                val optimalWeightFromPreviousCompletion = decreaseWeight(
                    incrementOverride = workoutLift.incrementOverride,
                    repRangeBottom = reps,
                    rpeTarget = workoutLift.rpeTarget,
                    result = result,
                )
                val optimalResult = (result as StandardSetResultDto).copy(
                    weight = optimalWeightFromPreviousCompletion,
                    reps = reps,
                    rpe = workoutLift.rpeTarget,
                )
                incrementWeight(workoutLift, optimalResult)
            } ?: incrementWeight(workoutLift, result)
        }
    }

    private fun getRepsForPreviousWorkout(repRangeBottom: Int, repRangeTop: Int, microCycle: Int): Int? {
        val fullRepRange = (repRangeTop downTo repRangeBottom).toList()

        return if (fullRepRange.isNotEmpty()) {
            val previousMicroCycle = microCycle - 1
            val index = previousMicroCycle % fullRepRange.size
            fullRepRange[index]
        } else null
    }

    private fun getRepRangePlaceholder(repRangeBottom: Int, repRangeTop: Int, microCycle: Int): String {
        val fullRepRange = (repRangeTop downTo repRangeBottom).toList()

        return if (fullRepRange.isNotEmpty()) {
            val index = microCycle % fullRepRange.size
            fullRepRange[index].toString()
        } else ""
    }

    private fun decrementForDeload(
        lift: GenericWorkoutLift,
        setData: SetResult,
        deloadWeek: Int,
    ): Float {
        val increment =  (lift.incrementOverride ?:
            SettingsManager.getSetting(SettingsManager.SettingNames.INCREMENT_AMOUNT,
                SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
            )).toInt()

        return setData.weight - (increment * (deloadWeek - 2))
    }
}