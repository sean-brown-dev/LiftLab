package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.Utils
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
        previousResultsForDisplay: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        if (workoutLift !is StandardWorkoutLiftDto) throw Exception ("Wave Loading progression cannot have custom sets")
        val groupedSetData = previousSetResults.associateBy { it.setPosition }
        val displaySetResults = previousResultsForDisplay.associateBy { it.setPosition }
        val setCount = if (isDeloadWeek) 2 else workoutLift.setCount
        val rpeTarget = if (isDeloadWeek) 6f else workoutLift.rpeTarget
        val deloadWeek = workoutLift.deloadWeek ?: programDeloadWeek

        return List(setCount) { setPosition ->
            val result = groupedSetData[setPosition]
            val displayResult = displaySetResults[setPosition]
            val weightRecommendation = if (!isDeloadWeek && result != null)
                getWeightRecommendation(workoutLift, result, deloadWeek)
            else if (result != null)
                decrementForDeload(lift = workoutLift, setData = result, deloadWeek = workoutLift.deloadWeek ?: programDeloadWeek)
            else null

            LoggingStandardSetDto(
                position = setPosition,
                rpeTarget = rpeTarget,
                repRangeBottom = workoutLift.repRangeBottom,
                repRangeTop = workoutLift.repRangeTop,
                previousSetResultLabel = getPreviousSetResultLabel(displayResult),
                repRangePlaceholder = if (!isDeloadWeek) {
                    getRepRangePlaceholder(
                        repRangeBottom = workoutLift.repRangeBottom,
                        repRangeTop = workoutLift.repRangeTop,
                        microCycle = microCycle,
                        deloadWeek = deloadWeek,
                        workoutLift.stepSize,
                    )
               } else {
                      workoutLift.repRangeBottom.toString()
                },
                weightRecommendation = weightRecommendation,
                hadInitialWeightRecommendation = weightRecommendation != null,
            )
        }.flattenWeightRecommendationsStandard()
    }

    private fun getWeightRecommendation(workoutLift: StandardWorkoutLiftDto, result: SetResult, deloadWeek: Int): Float? {
        val reps = getRepsForMicrocycle(
            repRangeBottom = workoutLift.repRangeBottom,
            repRangeTop = workoutLift.repRangeTop,
            microCycle = microCycle,
            deloadWeek = deloadWeek,
            workoutLift.stepSize,
        )
        val isTopOfRepRange = reps == workoutLift.repRangeTop
        val recalculateWeight = shouldRecalculateWeight(result, workoutLift, deloadWeek)

        return if (isTopOfRepRange && !recalculateWeight) {
            decrementForNewMicrocycle(workoutLift, result)
        } else if (!recalculateWeight) {
            incrementWeight(workoutLift, result)
        } else if (reps != null) {
            getCalculatedWeightRecommendation(
                increment = workoutLift.incrementOverride,
                repGoal = reps,
                rpeTarget = workoutLift.rpeTarget,
                result = result,
            )
        } else null
    }

    private fun getRepsForMicrocycle(repRangeBottom: Int, repRangeTop: Int, microCycle: Int, deloadWeek: Int, stepSize: Int?): Int? {
        return if (stepSize != null) {
            if (microCycle < deloadWeek - 1) {
                val steps = Utils.generateCompleteStepSequence(repRangeTop = repRangeTop, repRangeBottom = repRangeBottom, stepSize = stepSize, deloadWeek - 1)
                steps[microCycle]
            } else {
                repRangeBottom
            }
        } else {
            getRepsForMicrocycleWithUnevenStepSize(
                repRangeBottom = repRangeBottom,
                repRangeTop = repRangeTop,
                microCycle = microCycle,
                deloadWeek = deloadWeek
            )
        }
    }

    private fun getRepsForMicrocycleWithUnevenStepSize(repRangeBottom: Int, repRangeTop: Int, microCycle: Int, deloadWeek: Int): Int? {
        val fullRepRange = (repRangeTop downTo repRangeBottom).toList()

        return if (fullRepRange.isNotEmpty()) {
            val repsToStep = fullRepRange.size - 1
            val stepSize = maxOf(1, repsToStep / (deloadWeek - 2))

            // Deload week and the week before both should end on repRangeBottom
            val index = if (microCycle < deloadWeek - 2) {
                val thisStep = microCycle * stepSize
                // Cycle back to start when this step exceeds total step size but previous
                // microcycle did not
                if (thisStep <= repsToStep) thisStep
                else if ((microCycle - 1) * stepSize < repsToStep) repsToStep
                else 0
            } else repsToStep

            fullRepRange[index]
        } else null
    }

    private fun getRepRangePlaceholder(repRangeBottom: Int, repRangeTop: Int, microCycle: Int, deloadWeek: Int, stepSize: Int?): String {
        return getRepsForMicrocycle(
            repRangeBottom = repRangeBottom,
            repRangeTop = repRangeTop,
            microCycle = microCycle,
            deloadWeek = deloadWeek,
            stepSize = stepSize,
        )?.toString() ?: ""
    }

    private fun decrementForDeload(
        lift: StandardWorkoutLiftDto,
        setData: SetResult,
        deloadWeek: Int,
    ): Float {
        val increment = lift.incrementOverride ?:
            SettingsManager.getSetting(INCREMENT_AMOUNT, DEFAULT_INCREMENT_AMOUNT)

        return if (!shouldRecalculateWeight(result = setData, workoutLift = lift, deloadWeek = deloadWeek)) {
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

    private fun shouldRecalculateWeight(result: SetResult, workoutLift: StandardWorkoutLiftDto, deloadWeek: Int): Boolean {
        return if (microCycle == 0 && result.reps == workoutLift.repRangeBottom) {
            false
        } else if (microCycle > 0) {
            val repsForPreviousMicro = getRepsForMicrocycle(
                repRangeBottom = workoutLift.repRangeBottom,
                repRangeTop = workoutLift.repRangeTop,
                microCycle = microCycle - 1,
                deloadWeek = deloadWeek,
                workoutLift.stepSize,
            )
            repsForPreviousMicro != result.reps
        } else {
            // new microcycle but result's reps were not rep range bottom
            true
        }
    }
}