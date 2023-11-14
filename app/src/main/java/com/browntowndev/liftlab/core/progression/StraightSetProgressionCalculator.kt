package com.browntowndev.liftlab.core.progression

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingDropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingMyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.LoggingStandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLoggingSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

abstract class StraightSetProgressionCalculator: BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val sortedSetData = previousSetResults.sortedBy { it.setPosition }

        return calculateProgressions(workoutLift, sortedSetData, isDeloadWeek)
    }

    protected open fun getFailureWeight(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
        position: Int? = null,
    ): Float? {
        val result = previousSetResults.getOrNull(position ?: -1)
        return if (result == null) {
            previousSetResults.firstOrNull()?.weight
        } else {
            when (workoutLift) {
                is StandardWorkoutLiftDto -> getStandardWorkoutLiftFailureWeight(
                    workoutLift = workoutLift,
                    result = result,
                )
                is CustomWorkoutLiftDto -> getCustomWorkoutLiftFailureWeight(
                    incrementOverride = workoutLift.incrementOverride,
                    set = workoutLift.customLiftSets[position!!],
                    result = result,
                )
                else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
            }
        }
    }

    private fun getStandardWorkoutLiftFailureWeight(
        workoutLift: StandardWorkoutLiftDto,
        result: SetResult,
    ): Float {
        return if (shouldDecreaseWeight(result, workoutLift)) {
            decreaseWeight(
                incrementOverride = workoutLift.incrementOverride,
                repRangeBottom = workoutLift.repRangeBottom,
                rpeTarget = workoutLift.rpeTarget,
                prevSet = result
            )
        } else {
            result.weight
        }
    }

    private fun getCustomWorkoutLiftFailureWeight(
        incrementOverride: Float?,
        set: GenericLiftSet,
        result: SetResult,
    ): Float {
        val minimumRepsAllowed = set.repRangeBottom - 1
        val repsConsideringRpe = result.reps + (10 - result.rpe)
        val missedBottomRepRange = repsConsideringRpe < minimumRepsAllowed

        return if (missedBottomRepRange) {
            decreaseWeight(
                incrementOverride = incrementOverride,
                repRangeBottom = set.repRangeBottom,
                rpeTarget = set.rpeTarget,
                prevSet = result
            )
        } else {
            result.weight
        }
    }

    protected abstract fun allSetsMetCriterion(
        lift: StandardWorkoutLiftDto,
        previousSetResults: List<SetResult>,
    ): Boolean

    protected abstract fun allSetsMetCriterion(
        lift: CustomWorkoutLiftDto,
        previousSetResults: List<SetResult>,
    ): Boolean

    private fun allSetsMetCriterion(
        lift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
    ): Boolean {
        return previousSetResults.isNotEmpty() &&
                when (lift) {
                    is StandardWorkoutLiftDto -> allSetsMetCriterion(lift, previousSetResults)
                    is CustomWorkoutLiftDto -> allSetsMetCriterion(lift, previousSetResults)
                    else -> throw Exception("${lift::class.simpleName} is not defined.")
                }
    }

    private fun calculateProgressions(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val criterionMet = allSetsMetCriterion(workoutLift, previousSetResults)
        val nonMyoRepSetResults = previousSetResults
            .filterNot { it is MyoRepSetResultDto }
            .associateBy { it.setPosition }
        val myoRepSetResults = previousSetResults
            .filterIsInstance<MyoRepSetResultDto>()
            .groupBy { it.setPosition }

        // TODO: Unit tests
        return when (workoutLift) {
            is StandardWorkoutLiftDto -> {
                List(workoutLift.setCount) {
                    val result = nonMyoRepSetResults[it]
                    LoggingStandardSetDto(
                        position = it,
                        rpeTarget = workoutLift.rpeTarget,
                        repRangeBottom = workoutLift.repRangeBottom,
                        repRangeTop = workoutLift.repRangeTop,
                        previousSetResultLabel = getPreviousSetResultLabel(result),
                        repRangePlaceholder = if (!isDeloadWeek) {
                           "${workoutLift.repRangeBottom}-${workoutLift.repRangeTop}"
                        } else workoutLift.repRangeBottom.toString(),
                        weightRecommendation =
                        if (criterionMet) {
                            incrementWeight(workoutLift, result ?: previousSetResults.last())
                        } else if (previousSetResults.isNotEmpty()) {
                            getFailureWeight(
                                workoutLift = workoutLift,
                                previousSetResults = previousSetResults,
                                position = it,
                            )
                        } else null
                    )
                }
            }

            is CustomWorkoutLiftDto -> {
                var lastWeightRecommendation: Float? = null
                workoutLift.customLiftSets.flatMap { set ->
                    val result = nonMyoRepSetResults[set.position]
                    val currSetMyoRepResults = myoRepSetResults[set.position]
                    when (set) {
                        is StandardSetDto -> listOf(
                            LoggingStandardSetDto(
                            position = set.position,
                            rpeTarget = set.rpeTarget,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            previousSetResultLabel = getPreviousSetResultLabel(result),
                            repRangePlaceholder = if (!isDeloadWeek) {
                                "${set.repRangeBottom}-${set.repRangeTop}"
                            } else set.repRangeBottom.toString(),
                            weightRecommendation =
                            if (criterionMet) {
                                lastWeightRecommendation = incrementWeight(workoutLift, result ?: previousSetResults.last())
                                lastWeightRecommendation
                            } else if (previousSetResults.isNotEmpty()) {
                                lastWeightRecommendation = getFailureWeight(
                                    workoutLift = workoutLift,
                                    previousSetResults = previousSetResults,
                                    position = set.position,
                                )
                                lastWeightRecommendation
                            } else null
                        ))

                        is DropSetDto -> listOf(
                            LoggingDropSetDto(
                                position = set.position,
                                rpeTarget = set.rpeTarget,
                                repRangeBottom = set.repRangeBottom,
                                repRangeTop = set.repRangeTop,
                                previousSetResultLabel = getPreviousSetResultLabel(result),
                                repRangePlaceholder = if (!isDeloadWeek) {
                                    "${set.repRangeBottom}-${set.repRangeTop}"
                                } else set.repRangeBottom.toString(),
                                weightRecommendation = if (criterionMet) {
                                    lastWeightRecommendation = getDropSetRecommendation(workoutLift, set, lastWeightRecommendation)
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
                               },
                                dropPercentage = set.dropPercentage,
                        ))

                        is MyoRepSetDto -> {
                            (currSetMyoRepResults?.fastMap {
                                LoggingMyoRepSetDto(
                                    position = set.position,
                                    myoRepSetPosition = it.myoRepSetPosition,
                                    rpeTarget = set.rpeTarget,
                                    repRangeBottom = set.repRangeBottom,
                                    repRangeTop = set.repRangeTop,
                                    setMatching = set.setMatching,
                                    repFloor = set.repFloor,
                                    maxSets = set.maxSets,
                                    previousSetResultLabel = getPreviousSetResultLabel(result = it),
                                    repRangePlaceholder = if (!isDeloadWeek && it.myoRepSetPosition == null) {
                                        "${set.repRangeBottom}-${set.repRangeTop}"
                                    } else if (!isDeloadWeek && set.repFloor != null) {
                                        ">${set.repFloor}"
                                    } else if (!isDeloadWeek) {
                                        "—"
                                    } else {
                                        set.repRangeBottom.toString()
                                    },
                                    weightRecommendation = if (criterionMet) {
                                        incrementWeight(workoutLift, it)
                                    } else {
                                        getFailureWeight(
                                            workoutLift = workoutLift,
                                            previousSetResults = previousSetResults
                                        )
                                    }
                                )
                            }?.toMutableList() ?: mutableListOf()).apply {
                                if (size == 0) {
                                    add(
                                        LoggingMyoRepSetDto(
                                            position = set.position,
                                            rpeTarget = set.rpeTarget,
                                            repRangeBottom = set.repRangeBottom,
                                            repRangeTop = set.repRangeTop,
                                            setMatching = set.setMatching,
                                            repFloor = set.repFloor,
                                            maxSets = set.maxSets,
                                            previousSetResultLabel = getPreviousSetResultLabel(result = null),
                                            repRangePlaceholder = if (!isDeloadWeek) {
                                                "${set.repRangeBottom}-${set.repRangeTop}"
                                            } else {
                                                set.repRangeBottom.toString()
                                            },
                                            weightRecommendation = null,
                                        )
                                    )
                                }
                            }
                        }

                        else -> throw Exception("${set::class.simpleName} is not defined.")
                    }
                }
            }

            else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
        }.flattenWeightRecommendationsGeneric()
    }
}