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
        previousSetResults: List<SetResult>
    ): Float? {
        return previousSetResults.firstOrNull()?.weight
    }

    protected open fun getFailureWeight(
        workoutLift: GenericWorkoutLift,
        result: SetResult,
    ): Float? {
        return null
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
        val resultsByPosition = previousSetResults
            .filterNot { it is MyoRepSetResultDto }
            .associateBy { it.setPosition }
        val myoRepSetResults = previousSetResults
            .filterIsInstance<MyoRepSetResultDto>()
            .groupBy { it.setPosition }

        // TODO: Unit tests
        return when (workoutLift) {
            is StandardWorkoutLiftDto -> {
                List(workoutLift.setCount) {
                    val result = resultsByPosition[it]
                    LoggingStandardSetDto(
                        setPosition = it,
                        rpeTarget = workoutLift.rpeTarget,
                        repRangeBottom = workoutLift.repRangeBottom,
                        repRangeTop = workoutLift.repRangeTop,
                        previousSetResultLabel =
                        if (result != null) {
                            "${result.weight}x${result.reps} @${result.rpe}"
                        } else "—",
                        repRangePlaceholder = if (!isDeloadWeek) {
                           "${workoutLift.repRangeBottom}-${workoutLift.repRangeTop}"
                        } else workoutLift.repRangeBottom.toString(),
                        weightRecommendation =
                        if (criterionMet) {
                            incrementWeight(workoutLift, result ?: previousSetResults.last())
                        } else if (result != null) {
                            getFailureWeight(workoutLift = workoutLift, result = result)
                                ?: getFailureWeight(
                                    workoutLift = workoutLift,
                                    previousSetResults = previousSetResults
                                )
                        } else null
                    )
                }
            }

            is CustomWorkoutLiftDto -> {
                var lastWeightRecommendation: Float? = null
                workoutLift.customLiftSets.flatMap { set ->
                    val result = resultsByPosition[set.setPosition]
                    val currSetMyoRepResults = myoRepSetResults[set.setPosition]
                    when (set) {
                        is StandardSetDto -> listOf(
                            LoggingStandardSetDto(
                            setPosition = set.setPosition,
                            rpeTarget = set.rpeTarget,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            previousSetResultLabel =
                            if (result != null) {
                                "${result.weight}x${result.reps} @${result.rpe}"
                            } else "—",
                            repRangePlaceholder = if (!isDeloadWeek) {
                                "${set.repRangeBottom}-${set.repRangeTop}"
                            } else set.repRangeBottom.toString(),
                            weightRecommendation =
                            if (criterionMet) {
                                lastWeightRecommendation = incrementWeight(workoutLift, result ?: previousSetResults.last())
                                lastWeightRecommendation
                            } else if (previousSetResults.isNotEmpty()) {
                                getFailureWeight(workoutLift = workoutLift, result = result ?: previousSetResults.last())
                                    ?: getFailureWeight(
                                        workoutLift = workoutLift,
                                        previousSetResults = previousSetResults
                                    )
                            } else null
                        ))

                        is DropSetDto -> listOf(
                            LoggingDropSetDto(
                            setPosition = set.setPosition,
                            rpeTarget = set.rpeTarget,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            previousSetResultLabel =
                            if (result != null) {
                                "${result.weight}x${result.reps} @${result.rpe}"
                            } else "—",
                            repRangePlaceholder = if (!isDeloadWeek) {
                                "${set.repRangeBottom}-${set.repRangeTop}"
                            } else set.repRangeBottom.toString(),
                            weightRecommendation = if (criterionMet) {
                                lastWeightRecommendation = getDropSetRecommendation(workoutLift, set, lastWeightRecommendation)
                                lastWeightRecommendation
                            } else if (previousSetResults.isNotEmpty()) {
                                previousSetResults[set.setPosition].weight
                            } else null,
                            dropPercentage = set.dropPercentage,
                        ))

                        is MyoRepSetDto -> {
                            (currSetMyoRepResults?.fastMap {
                                LoggingMyoRepSetDto(
                                    setPosition = set.setPosition,
                                    myoRepSetPosition = it.myoRepSetPosition,
                                    rpeTarget = set.rpeTarget,
                                    repRangeBottom = set.repRangeBottom,
                                    repRangeTop = set.repRangeTop,
                                    setMatching = set.setMatching,
                                    repFloor = set.repFloor,
                                    maxSets = set.maxSets,
                                    previousSetResultLabel = "${it.weight}x${it.reps} @${it.rpe}",
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
                                        getFailureWeight(workoutLift = workoutLift, result = it)
                                            ?: getFailureWeight(
                                                workoutLift = workoutLift,
                                                previousSetResults = previousSetResults
                                            )
                                    }
                                )
                            }?.toMutableList() ?: mutableListOf()).apply {
                                if (size == 0) {
                                    add(
                                        LoggingMyoRepSetDto(
                                            setPosition = set.setPosition,
                                            rpeTarget = set.rpeTarget,
                                            repRangeBottom = set.repRangeBottom,
                                            repRangeTop = set.repRangeTop,
                                            setMatching = set.setMatching,
                                            repFloor = set.repFloor,
                                            maxSets = set.maxSets,
                                            previousSetResultLabel = "—",
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
        }
    }
}