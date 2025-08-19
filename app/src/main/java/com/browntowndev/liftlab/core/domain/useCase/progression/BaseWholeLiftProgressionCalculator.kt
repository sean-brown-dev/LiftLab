package com.browntowndev.liftlab.core.domain.useCase.progression

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.extensions.getRpeTarget
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationCustomLiftSet
import com.browntowndev.liftlab.core.domain.models.interfaces.CalculationWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationDropSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutCalculation.CalculationStandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingDropSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingMyoRepSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingStandardSet
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.utils.exceededRepRangeTop
import com.browntowndev.liftlab.core.domain.utils.missedRepRangeBottom

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
                List(setCount) { index ->
                    val result = nonMyoRepSetResults[index]
                    val displayResult = displayResults["${index}-null"]

                    val rpeTarget = getRpeTarget(
                        setIndex = index,
                        setCount = setCount,
                        progressionScheme = workoutLift.progressionScheme,
                        topSetRpeTarget = workoutLift.rpeTarget
                    )
                    val weightRecommendation = getWeightRecommendation(
                        workoutLift = workoutLift,
                        rpeTarget = rpeTarget,
                        repRangeTop = workoutLift.repRangeTop,
                        repRangeBottom = workoutLift.repRangeBottom,
                        previousSetResults = previousSetResults,
                        index = index,
                        criterionMet = criterionMet,
                        result = result
                    )

                    LoggingStandardSet(
                        position = index,
                        rpeTarget = getRpeTarget(
                            setIndex = index,
                            setCount = setCount,
                            progressionScheme = workoutLift.progressionScheme,
                            topSetRpeTarget = workoutLift.rpeTarget
                        ),
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
                                val weightRecommendation = getWeightRecommendation(
                                    workoutLift = workoutLift,
                                    rpeTarget = set.rpeTarget,
                                    repRangeTop = set.repRangeTop,
                                    repRangeBottom = set.repRangeBottom,
                                    previousSetResults = previousSetResults,
                                    index = set.position,
                                    criterionMet = criterionMet,
                                    result = result,
                                )
                                lastWeightRecommendation = weightRecommendation

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
                                val weightRecommendation = getDropSetWeightRecommendation(
                                    workoutLift = workoutLift,
                                    set = set,
                                    result = result,
                                    criterionMet = criterionMet,
                                    lastWeightRecommendation = lastWeightRecommendation,
                                    nonMyoRepSetResults = nonMyoRepSetResults
                                )
                                lastWeightRecommendation = weightRecommendation

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
                                    val weightRecommendation = getWeightRecommendation(
                                        workoutLift = workoutLift,
                                        rpeTarget = set.rpeTarget,
                                        repRangeTop = set.repRangeTop,
                                        repRangeBottom = set.repRangeBottom,
                                        previousSetResults = previousSetResults,
                                        index = set.position,
                                        criterionMet = criterionMet,
                                        result = it
                                    )

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

    protected fun getDropSetWeightRecommendation(
        workoutLift: CalculationWorkoutLift,
        set: CalculationDropSet,
        result: SetResult?,
        criterionMet: Boolean,
        lastWeightRecommendation: Float?,
        nonMyoRepSetResults: Map<Int, SetResult?>
    ): Float? {

        val exceededRepRangeTop = if (result != null) {
            exceededRepRangeTop(
                repRangeTop = set.repRangeTop,
                rpeTarget = set.rpeTarget,
                completedReps = result.reps,
                completedRpe = result.rpe,
            )
        } else false

        return when {
            exceededRepRangeTop -> getCalculatedWeightRecommendation(
                increment = workoutLift.incrementOverride,
                repGoal = set.repRangeBottom,
                rpeTarget = set.rpeTarget,
                result = result!!
            )
            criterionMet -> getDropSetRecommendation(
                workoutLift,
                set,
                previousSetWeight = lastWeightRecommendation
            )
            else -> getDropSetFailureWeight(
                incrementOverride = workoutLift.incrementOverride,
                repRangeBottom = set.repRangeBottom,
                rpeTarget = set.rpeTarget,
                dropPercentage = set.dropPercentage,
                result = result,
                droppedFromSetResult = nonMyoRepSetResults.getOrDefault(set.position - 1, null),
            )
        }
    }

    protected fun getWeightRecommendation(
        workoutLift: CalculationWorkoutLift,
        rpeTarget: Float,
        repRangeTop: Int,
        repRangeBottom: Int,
        previousSetResults: List<SetResult>,
        index: Int,
        criterionMet: Boolean,
        result: SetResult?
    ): Float? {
        val exceededRepRangeTop = if (result != null) {
            exceededRepRangeTop(
                repRangeTop = repRangeTop,
                rpeTarget = rpeTarget,
                completedReps = result.reps,
                completedRpe = result.rpe,
            )
        } else false

        val weightRecommendation = when {
            exceededRepRangeTop -> getCalculatedWeightRecommendation(
                increment = workoutLift.incrementOverride,
                repGoal = repRangeBottom,
                rpeTarget = rpeTarget,
                result = result!!
            )
            criterionMet -> incrementWeight(workoutLift, result ?: previousSetResults.last())
            previousSetResults.isNotEmpty() -> getFailureWeight(
                workoutLift = workoutLift,
                previousSetResults = previousSetResults,
                position = index,
            )
            else -> null
        }

        return weightRecommendation
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
            missedRepRangeBottom(
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