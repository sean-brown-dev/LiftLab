package com.browntowndev.liftlab.core.domain.useCase.workout.progression

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.DropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.StandardSet
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationCustomLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationDropSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardWorkoutLift

abstract class BaseWholeLiftProgressionCalculator: BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: CalculationWorkoutLift,
        previousSetResults: List<SetResult>,
        previousResultsForDisplay: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val criterionMet = allSetsMetCriterion(workoutLift, previousSetResults)
        val displayResults = previousResultsForDisplay
            .associateBy { "${it.setPosition}-${(it as? MyoRepSetResult)?.myoRepSetPosition}" }
        val nonMyoRepSetResults = previousSetResults
            .filterNot { it is MyoRepSetResult }
            .associateBy { it.setPosition }
        val myoRepSetResults = previousSetResults
            .filterIsInstance<MyoRepSetResult>()
            .groupBy { it.setPosition }

        // TODO: Unit tests
        return when (workoutLift) {
            is CalculationStandardWorkoutLift -> {
                val setCount = if (isDeloadWeek) 2 else workoutLift.setCount
                List(setCount) {
                    val result = nonMyoRepSetResults[it]
                    val displayResult = displayResults["${it}-null"]
                    val weightRecommendation = if (criterionMet) {
                        incrementWeight(workoutLift, result ?: previousSetResults.last())
                    } else if (previousSetResults.isNotEmpty()) {
                        getFailureWeight(
                            workoutLift = workoutLift,
                            previousSetResults = previousSetResults,
                            position = it,
                        )
                    } else null
                    LoggingStandardSet(
                        position = it,
                        rpeTarget = workoutLift.rpeTarget,
                        repRangeBottom = workoutLift.repRangeBottom,
                        repRangeTop = workoutLift.repRangeTop,
                        previousSetResultLabel = getPreviousSetResultLabel(displayResult),
                        repRangePlaceholder = if (!isDeloadWeek) {
                            "${workoutLift.repRangeBottom}-${workoutLift.repRangeTop}"
                        } else workoutLift.repRangeBottom.toString(),
                        weightRecommendation = weightRecommendation,
                        hadInitialWeightRecommendation = weightRecommendation != null,
                    )
                }
            }

            is CalculationCustomWorkoutLift -> {
                if (!isDeloadWeek) {
                    var lastWeightRecommendation: Float? = null
                    workoutLift.customLiftSets.flatMap { set ->
                        val result = nonMyoRepSetResults[set.position]
                        val displayResult = displayResults["${set.position}-null"]
                        val currSetMyoRepResults = myoRepSetResults[set.position]
                        when (set) {
                            is CalculationStandardSet -> {
                                val weightRecommendation =
                                    if (criterionMet) {
                                        lastWeightRecommendation = incrementWeight(
                                            workoutLift,
                                            result ?: previousSetResults.last()
                                        )
                                        lastWeightRecommendation
                                    } else if (previousSetResults.isNotEmpty()) {
                                        lastWeightRecommendation = getFailureWeight(
                                            workoutLift = workoutLift,
                                            previousSetResults = previousSetResults,
                                            position = set.position,
                                        )
                                        lastWeightRecommendation
                                    } else null

                                listOf(
                                    LoggingStandardSet(
                                        position = set.position,
                                        rpeTarget = set.rpeTarget,
                                        repRangeBottom = set.repRangeBottom,
                                        repRangeTop = set.repRangeTop,
                                        previousSetResultLabel = getPreviousSetResultLabel(displayResult),
                                        repRangePlaceholder = "${set.repRangeBottom}-${set.repRangeTop}",
                                        weightRecommendation = weightRecommendation,
                                        hadInitialWeightRecommendation = weightRecommendation != null,
                                    )
                                )
                            }

                            is CalculationDropSet -> {
                                val weightRecommendation = if (criterionMet) {
                                    lastWeightRecommendation = getDropSetRecommendation(
                                        workoutLift,
                                        set,
                                        lastWeightRecommendation
                                    )
                                    lastWeightRecommendation
                                } else {
                                    getDropSetFailureWeight(
                                        incrementOverride = workoutLift.incrementOverride,
                                        repRangeBottom = set.repRangeBottom,
                                        rpeTarget = set.rpeTarget,
                                        dropPercentage = set.dropPercentage,
                                        result = result,
                                        droppedFromSetResult = nonMyoRepSetResults
                                            .getOrDefault(set.position - 1, null),
                                    )
                                }
                                listOf(
                                    LoggingDropSet(
                                        position = set.position,
                                        rpeTarget = set.rpeTarget,
                                        repRangeBottom = set.repRangeBottom,
                                        repRangeTop = set.repRangeTop,
                                        dropPercentage = set.dropPercentage,
                                        previousSetResultLabel = getPreviousSetResultLabel(displayResult),
                                        repRangePlaceholder = "${set.repRangeBottom}-${set.repRangeTop}",
                                        weightRecommendation = weightRecommendation,
                                        hadInitialWeightRecommendation = weightRecommendation != null,
                                    )
                                )
                            }

                            is CalculationMyoRepSet -> {
                                (currSetMyoRepResults?.fastMap {
                                    val myoRepDisplayResult = displayResults["${it.setPosition}-${it.myoRepSetPosition}"]
                                    val weightRecommendation = if (criterionMet) {
                                        incrementWeight(workoutLift, it)
                                    } else {
                                        getFailureWeight(
                                            workoutLift = workoutLift,
                                            previousSetResults = previousSetResults
                                        )
                                    }
                                    LoggingMyoRepSet(
                                        position = set.position,
                                        myoRepSetPosition = it.myoRepSetPosition,
                                        rpeTarget = if (it.myoRepSetPosition == null) set.rpeTarget else 10f,
                                        repRangeBottom = set.repRangeBottom,
                                        repRangeTop = set.repRangeTop,
                                        setMatching = set.setMatching,
                                        repFloor = set.repFloor,
                                        maxSets = set.maxSets,
                                        previousSetResultLabel = getPreviousSetResultLabel(result = myoRepDisplayResult),
                                        repRangePlaceholder = if (it.myoRepSetPosition == null) {
                                            "${set.repRangeBottom}-${set.repRangeTop}"
                                        } else if (set.repFloor != null) {
                                            ">${set.repFloor}"
                                        } else {
                                            "—"
                                        },
                                        weightRecommendation = weightRecommendation,
                                        hadInitialWeightRecommendation = weightRecommendation != null,
                                    )
                                }?.toMutableList() ?: mutableListOf()).apply {
                                    if (isEmpty()) {
                                        add(
                                            LoggingMyoRepSet(
                                                position = set.position,
                                                rpeTarget = set.rpeTarget,
                                                repRangeBottom = set.repRangeBottom,
                                                repRangeTop = set.repRangeTop,
                                                setMatching = set.setMatching,
                                                repFloor = set.repFloor,
                                                maxSets = set.maxSets,
                                                previousSetResultLabel = getPreviousSetResultLabel(
                                                    result = null
                                                ),
                                                repRangePlaceholder = "${set.repRangeBottom}-${set.repRangeTop}",
                                                weightRecommendation = null,
                                                hadInitialWeightRecommendation = false,
                                            )
                                        )
                                    }
                                }
                            }

                            else -> throw Exception("${set::class.simpleName} is not defined.")
                        }
                    }
                } else {
                    val topStandard = workoutLift.customLiftSets.filterIsInstance<CalculationStandardSet>().firstOrNull()
                    return calculate(
                        workoutLift = CalculationStandardWorkoutLift(
                            id = workoutLift.id,
                            liftId = workoutLift.liftId,
                            position = workoutLift.position,
                            setCount = workoutLift.setCount,
                            progressionScheme = workoutLift.progressionScheme,
                            incrementOverride = workoutLift.incrementOverride,
                            deloadWeek = workoutLift.deloadWeek,
                            rpeTarget = 6f,
                            repRangeBottom = topStandard?.repRangeBottom ?: 8,
                            repRangeTop = topStandard?.repRangeTop ?: 10,
                        ),
                        previousSetResults = previousSetResults,
                        previousResultsForDisplay = previousResultsForDisplay,
                        isDeloadWeek =  true,
                    )
                }
            }

            else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
        }.flattenWeightRecommendationsGeneric()
    }

    protected open fun getFailureWeight(
        workoutLift: CalculationWorkoutLift,
        previousSetResults: List<SetResult>,
        position: Int? = null,
    ): Float? {
        val result = previousSetResults.getOrNull(position ?: -1)
        return if (result == null) {
            previousSetResults.firstOrNull()?.weight
        } else {
            when (workoutLift) {
                is CalculationStandardWorkoutLift -> getStandardWorkoutLiftFailureWeight(
                    workoutLift = workoutLift,
                    result = result,
                )
                is CalculationCustomWorkoutLift -> getCustomWorkoutLiftFailureWeight(
                    incrementOverride = workoutLift.incrementOverride,
                    set = workoutLift.customLiftSets[position!!],
                    result = result,
                )
                else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
            }
        }
    }

    private fun getStandardWorkoutLiftFailureWeight(
        workoutLift: CalculationStandardWorkoutLift,
        result: SetResult,
    ): Float {
        return if (missedBottomRepRange(result, workoutLift)) {
            getCalculatedWeightRecommendation(
                increment = workoutLift.incrementOverride,
                repGoal = workoutLift.repRangeBottom,
                rpeTarget = workoutLift.rpeTarget,
                result = result
            )
        } else {
            result.weight
        }
    }

    private fun getCustomWorkoutLiftFailureWeight(
        incrementOverride: Float?,
        set: CalculationCustomLiftSet,
        result: SetResult,
    ): Float {
        return if (
            missedBottomRepRange(
                repRangeBottom = set.repRangeBottom,
                rpeTarget = set.rpeTarget,
                completedReps = result.reps,
                completedRpe = result.rpe,
            )
        ) {
            getCalculatedWeightRecommendation(
                increment = incrementOverride,
                repGoal = set.repRangeBottom,
                rpeTarget = set.rpeTarget,
                result = result
            )
        } else {
            result.weight
        }
    }

    protected abstract fun allSetsMetCriterion(
        lift: CalculationStandardWorkoutLift,
        previousSetResults: List<SetResult>,
    ): Boolean

    protected abstract fun allSetsMetCriterion(
        lift: CalculationCustomWorkoutLift,
        previousSetResults: List<SetResult>,
    ): Boolean

    private fun allSetsMetCriterion(
        lift: CalculationWorkoutLift,
        previousSetResults: List<SetResult>,
    ): Boolean {
        return previousSetResults.isNotEmpty() &&
                when (lift) {
                    is CalculationStandardWorkoutLift -> allSetsMetCriterion(lift, previousSetResults)
                    is CalculationCustomWorkoutLift -> allSetsMetCriterion(lift, previousSetResults)
                    else -> throw Exception("${lift::class.simpleName} is not defined.")
                }
    }
}