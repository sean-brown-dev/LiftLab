package com.browntowndev.liftlab.core.domain.useCase.progression

import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.extensions.getRpeTarget
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.utils.calculateMissedGoalResult
import com.browntowndev.liftlab.core.domain.utils.generateCompleteStepSequence

class WaveLoadingProgressionCalculator(
    private val programDeloadWeek: Int,
    private val microCycle: Int
): BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: CalculationWorkoutLift,
        previousSetResults: List<SetResult>,
        previousResultsForDisplay: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        if (workoutLift !is CalculationStandardWorkoutLift) throw Exception ("Wave Loading progression cannot have custom sets")
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

            LoggingStandardSet(
                position = setPosition,
                rpeTarget = getRpeTarget(
                    setIndex = setPosition,
                    setCount = setCount,
                    progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                    topSetRpeTarget = rpeTarget
                ),
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

    private fun getWeightRecommendation(workoutLift: CalculationStandardWorkoutLift, result: SetResult, deloadWeek: Int): Float? {
        val reps = getRepsForMicrocycle(
            repRangeBottom = workoutLift.repRangeBottom,
            repRangeTop = workoutLift.repRangeTop,
            microCycle = microCycle,
            deloadWeek = deloadWeek,
            stepSize = workoutLift.stepSize,
        )
        val isTopOfRepRange = reps == workoutLift.repRangeTop
        val recalculateWeight = shouldRecalculateWeight(result, workoutLift, deloadWeek)

        return if (isTopOfRepRange && !recalculateWeight) {
            decrementForNewMicrocycle(workoutLift, result)
        } else if (!recalculateWeight) {
            incrementWeight(workoutLift, result)
        } else {
            val rpeTarget = getRpeTarget(
                setIndex = result.setPosition,
                setCount = workoutLift.setCount,
                progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
                topSetRpeTarget = workoutLift.rpeTarget
            )
            getCalculatedWeightRecommendation(
                increment = workoutLift.incrementOverride,
                repGoal = reps,
                rpeTarget = rpeTarget,
                result = result,
            )
        }
    }

    private fun getRepsForMicrocycle(repRangeBottom: Int, repRangeTop: Int, microCycle: Int, deloadWeek: Int, stepSize: Int?): Int {
        return if (stepSize != null) {
            if (microCycle < deloadWeek - 1) {
                val steps = generateCompleteStepSequence(
                    repRangeTop = repRangeTop,
                    repRangeBottom = repRangeBottom,
                    stepSize = stepSize,
                    totalStepsToTake = deloadWeek - 1,
                )
                steps[microCycle]
            } else {
                repRangeBottom
            }
        } else {
            getRepsForMicrocycleWithUnevenStepSize(
                repRangeBottom = repRangeBottom,
                repRangeTop = repRangeTop,
                microCycle = microCycle,
                deloadWeek = deloadWeek,
            )
        }
    }

    private fun getRepsForMicrocycleWithUnevenStepSize(repRangeBottom: Int, repRangeTop: Int, microCycle: Int, deloadWeek: Int): Int {
        if (repRangeTop <= repRangeBottom) {
            error("Invalid rep range. Top must be greater than bottom.")
        }

        val fullRepRange = (repRangeTop downTo repRangeBottom).toList()

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

        return fullRepRange[index]
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
        lift: CalculationStandardWorkoutLift,
        setData: SetResult,
        deloadWeek: Int,
    ): Float {
        val increment = lift.incrementOverride ?: defaultIncrement

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
        lift: CalculationStandardWorkoutLift,
        setData: SetResult,
    ): Float {
        val increment =  lift.incrementOverride ?: defaultIncrement

        return setData.weight - increment
    }

    private fun shouldRecalculateWeight(result: SetResult, workoutLift: CalculationStandardWorkoutLift, deloadWeek: Int): Boolean {
        val rpeTarget = getRpeTarget(
            setIndex = result.setPosition,
            setCount = workoutLift.setCount,
            progressionScheme = ProgressionScheme.WAVE_LOADING_PROGRESSION,
            topSetRpeTarget = workoutLift.rpeTarget
        )

        val previousMicroCycle = if (microCycle > 0) microCycle - 1 else deloadWeek - 1
        val repsForPreviousMicroBasedOnCurrentSettings = getRepsForMicrocycle(
            repRangeBottom = workoutLift.repRangeBottom,
            repRangeTop = workoutLift.repRangeTop,
            microCycle = previousMicroCycle,
            deloadWeek = deloadWeek,
            stepSize = workoutLift.stepSize,
        )

        val missedRepRangeResult = calculateMissedGoalResult(
            repRangeBottom = repsForPreviousMicroBasedOnCurrentSettings,
            repRangeTop = repsForPreviousMicroBasedOnCurrentSettings,
            rpeTarget = rpeTarget,
            completedReps = result.reps,
            completedRpe = result.rpe,
        )

        return when {
            // Set was too easy or too hard
            missedRepRangeResult.missedRepRangeBottom || missedRepRangeResult.exceededRepRangeTop -> true

            // New microcycle. Check to see if the last completed set's reps were at the bottom of the range.
            // If not, the plan has changed since then
            microCycle == 0 -> result.reps == workoutLift.repRangeBottom

            // Check if reps changed from previous microcycle
            microCycle > 0 -> {
                repsForPreviousMicroBasedOnCurrentSettings != result.reps
            }

            // First microcycle
            else -> false
        }
    }
}