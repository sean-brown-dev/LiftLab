package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
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

    private fun getWeightRecommendation(workoutLift: GenericWorkoutLift, result: SetResult): Float? {
        val recalculateWeight = shouldRecalculateWeight(result, workoutLift as StandardWorkoutLiftDto)

        return if (microCycle == 0 && !recalculateWeight) {
            decrementForNewMicrocycle(workoutLift, result)
        } else if (!recalculateWeight) {
            incrementWeight(workoutLift, result)
        } else {
            getRepsForMicrocycle(
                repRangeBottom = workoutLift.repRangeBottom,
                repRangeTop = workoutLift.repRangeTop,
                microCycle = microCycle,
            )?.let { reps ->
                getCalculatedWeightRecommendation(
                    increment = workoutLift.incrementOverride,
                    repGoal = reps,
                    rpeTarget = workoutLift.rpeTarget,
                    result = result,
                )
            }
        }
    }

    private fun getRepsForMicrocycle(repRangeBottom: Int, repRangeTop: Int, microCycle: Int): Int? {
        val fullRepRange = (repRangeTop downTo repRangeBottom).toList()

        return if (fullRepRange.isNotEmpty()) {
            val index = microCycle % fullRepRange.size
            fullRepRange[index]
        } else null
    }

    private fun getRepRangePlaceholder(repRangeBottom: Int, repRangeTop: Int, microCycle: Int): String {
        return getRepsForMicrocycle(
            repRangeBottom,
            repRangeTop = repRangeTop,
            microCycle = microCycle
        )?.toString() ?: ""
    }

    private fun decrementForDeload(
        lift: StandardWorkoutLiftDto,
        setData: SetResult,
        deloadWeek: Int,
    ): Float {
        val increment = lift.incrementOverride ?:
            SettingsManager.getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT)

        return if (!shouldRecalculateWeight(result = setData, workoutLift = lift)) {
            return setData.weight - (increment * (deloadWeek - 2))
        } else {
            getCalculatedWeightRecommendation(
                increment = increment,
                repGoal = lift.repRangeBottom,
                rpeTarget = 6f,
                result = setData,
            )
        }
    }

    private fun decrementForNewMicrocycle(
        lift: GenericWorkoutLift,
        setData: SetResult,
    ): Float {
        val increment =  lift.incrementOverride ?:
            SettingsManager.getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT)

        return setData.weight - increment
    }

    private fun shouldRecalculateWeight(result: SetResult, workoutLift: StandardWorkoutLiftDto): Boolean {
        return if (microCycle == 0 && result.reps == workoutLift.repRangeBottom) {
            false
        } else if (microCycle > 0) {
            val repsForPreviousMicro = getRepsForMicrocycle(
                repRangeBottom = workoutLift.repRangeBottom,
                repRangeTop = workoutLift.repRangeTop,
                microCycle = microCycle - 1,
            )
            repsForPreviousMicro != result.reps
        } else {
            // new microcycle but result's reps were not rep range bottom
            true
        }
    }
}