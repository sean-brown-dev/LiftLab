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

abstract class BaseWholeLiftProgressionCalculator: BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
        previousResultsForDisplay: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val criterionMet = allSetsMetCriterion(workoutLift, previousSetResults)
        val displayResults = previousResultsForDisplay
            .associateBy { "${it.setPosition}-${(it as? MyoRepSetResultDto)?.myoRepSetPosition}" }
        val nonMyoRepSetResults = previousSetResults
            .filterNot { it is MyoRepSetResultDto }
            .associateBy { it.setPosition }
        val myoRepSetResults = previousSetResults
            .filterIsInstance<MyoRepSetResultDto>()
            .groupBy { it.setPosition }

        // TODO: Unit tests
        return when (workoutLift) {
            is StandardWorkoutLiftDto -> {
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
                    LoggingStandardSetDto(
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

            is CustomWorkoutLiftDto -> {
                if (!isDeloadWeek) {
                    var lastWeightRecommendation: Float? = null
                    workoutLift.customLiftSets.flatMap { set ->
                        val result = nonMyoRepSetResults[set.position]
                        val displayResult = displayResults["${set.position}-null"]
                        val currSetMyoRepResults = myoRepSetResults[set.position]
                        when (set) {
                            is StandardSetDto -> {
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
                                    LoggingStandardSetDto(
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

                            is DropSetDto -> {
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
                                    LoggingDropSetDto(
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

                            is MyoRepSetDto -> {
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
                                    LoggingMyoRepSetDto(
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
                    val topStandard = workoutLift.customLiftSets.filterIsInstance<StandardSetDto>().firstOrNull()
                    return calculate(
                        workoutLift = StandardWorkoutLiftDto(
                            id = workoutLift.id,
                            workoutId = workoutLift.workoutId,
                            liftId = workoutLift.liftId,
                            liftName = workoutLift.liftName,
                            liftMovementPattern = workoutLift.liftMovementPattern,
                            liftVolumeTypes = workoutLift.liftVolumeTypes,
                            liftSecondaryVolumeTypes = workoutLift.liftSecondaryVolumeTypes,
                            position = workoutLift.position,
                            setCount = workoutLift.setCount,
                            progressionScheme = workoutLift.progressionScheme,
                            incrementOverride = workoutLift.incrementOverride,
                            restTime = workoutLift.restTime,
                            restTimerEnabled = workoutLift.restTimerEnabled,
                            deloadWeek = workoutLift.deloadWeek,
                            rpeTarget = 6f,
                            repRangeBottom = topStandard?.repRangeBottom ?: 8,
                            repRangeTop = topStandard?.repRangeTop ?: 10,
                            note = workoutLift.note,
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
        set: GenericLiftSet,
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
}