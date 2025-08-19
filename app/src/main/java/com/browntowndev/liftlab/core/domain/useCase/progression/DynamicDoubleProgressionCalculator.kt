package com.browntowndev.liftlab.core.domain.useCase.progression

import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.DEFAULT_INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.roundToNearestFactor
import com.browntowndev.liftlab.core.domain.enums.SetType
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

class DynamicDoubleProgressionCalculator: BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: CalculationWorkoutLift,
        previousSetResults: List<SetResult>,
        previousResultsForDisplay: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        return when (workoutLift) {
            is CalculationStandardWorkoutLift -> {
                getStandardSetProgressions(
                    workoutLift,
                    previousSetResults,
                    previousResultsForDisplay,
                    isDeloadWeek
                )
            }

            is CalculationCustomWorkoutLift -> {
                getCustomSetProgressions(
                    workoutLift,
                    previousSetResults,
                    previousResultsForDisplay,
                    isDeloadWeek
                )
            }

            else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
        }
    }

    private fun getStandardSetProgressions(
        workoutLift: CalculationStandardWorkoutLift,
        setResults: List<SetResult>,
        displayResults: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<LoggingStandardSet> {
        val resultsMap = setResults.associateBy { it.setPosition }
        val displayResultsMap = displayResults.associateBy { it.setPosition }
        val setCount = if (isDeloadWeek) 2 else workoutLift.setCount

        return List(setCount) { setPosition ->
            val result = resultsMap[setPosition]
            val displayResult = displayResultsMap[setPosition]

            val exceededRepRangeTop = result != null && exceededRepRangeTop(
                repRangeTop = workoutLift.repRangeTop,
                rpeTarget = workoutLift.rpeTarget,
                completedReps = result.reps,
                completedRpe = result.rpe,
            )
            val recalculateWeight = exceededRepRangeTop || missedBottomRepRange(result, workoutLift)
            val weightRecommendation = when {
                recalculateWeight -> getCalculatedWeightRecommendation(
                    increment = workoutLift.incrementOverride,
                    repGoal = workoutLift.repRangeBottom,
                    rpeTarget = workoutLift.rpeTarget,
                    result = result!!,
                )
                setMetCriterion(result, workoutLift) -> incrementWeight(workoutLift, result!!)
                else -> result?.weight
            }

            LoggingStandardSet(
                position = setPosition,
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

    private fun setMetCriterion(result: SetResult?, goals: CalculationStandardWorkoutLift): Boolean {
        return result != null &&
                result.reps >= goals.repRangeTop &&
                result.rpe <= goals.rpeTarget
    }

    private fun getCustomSetProgressions(
        workoutLift: CalculationCustomWorkoutLift,
        setResults: List<SetResult>,
        displayResults: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val displayResultsMap = displayResults.associateBy { "${it.setPosition}-${(it as? MyoRepSetResult)?.myoRepSetPosition}" }
        val standardSetResults = setResults
            .filterNot { it is MyoRepSetResult || it.setType == SetType.DROP_SET }
            .associateBy { it.setPosition }
        val lastStandardSetResult = standardSetResults.values.lastOrNull()

        val dropSetResults = buildDropSetWeightRecommendationsMap(
            workoutLift = workoutLift,
            setResults = setResults,
        )

        val myoRepSetResults = setResults
            .filterIsInstance<MyoRepSetResult>()
            .groupBy { it.setPosition }

        return if(!isDeloadWeek) {
            workoutLift.customLiftSets.flatMap { set ->
                when (set) {
                    is CalculationMyoRepSet -> {
                        val allMyoRepSetResults = myoRepSetResults[set.position]
                        val weightRecommendation =
                            getWeightRecommendation(
                                lift = workoutLift,
                                set = set,
                                setData = myoRepSetResults[set.position],
                                lastCompletedStandardSetResult = lastStandardSetResult)

                        (allMyoRepSetResults?.fastMap {
                            val displayResult = displayResultsMap["${it.setPosition}-${it.myoRepSetPosition}"]
                            LoggingMyoRepSet(
                                position = set.position,
                                myoRepSetPosition = it.myoRepSetPosition,
                                rpeTarget = if (it.myoRepSetPosition == null) set.rpeTarget else 10f,
                                repRangeBottom = set.repRangeBottom,
                                repRangeTop = set.repRangeTop,
                                setMatching = set.setMatching,
                                repFloor = set.repFloor,
                                maxSets = set.maxSets,
                                previousSetResultLabel = getPreviousSetResultLabel(result = displayResult),
                                weightRecommendation = weightRecommendation,
                                hadInitialWeightRecommendation = weightRecommendation != null,
                                repRangePlaceholder = if (it.myoRepSetPosition == null) {
                                    "${set.repRangeBottom}-${set.repRangeTop}"
                                } else if (set.repFloor != null) {
                                    ">${set.repFloor}"
                                } else {
                                    "—"
                                },
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
                                        maxSets = set.maxSets,
                                        repFloor = set.repFloor,
                                        previousSetResultLabel = getPreviousSetResultLabel(result = null),
                                        repRangePlaceholder = "${set.repRangeBottom}-${set.repRangeTop}",
                                        weightRecommendation = weightRecommendation,
                                        hadInitialWeightRecommendation = weightRecommendation != null,
                                    )
                                )
                            }
                        }
                    }

                    is CalculationDropSet -> {
                        val displayResult = displayResultsMap["${set.position}-null"]
                        val weightRecommendation = dropSetResults[set.id]
                        listOf(
                            LoggingDropSet(
                                dropPercentage = set.dropPercentage,
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

                    is CalculationStandardSet -> {
                        val result = standardSetResults[set.position]
                        val displayResult = displayResultsMap["${set.position}-null"]
                        val weightRecommendation = dropSetResults[set.id]
                            ?: getWeightRecommendation(
                                lift = workoutLift,
                                set = set,
                                setData = result,
                                lastCompletedStandardSetResult = lastStandardSetResult,
                            )
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

                    else -> throw Exception("${set::class.simpleName} is not defined.")
                }
            }
        } else {
            val topStandard = workoutLift.customLiftSets.filterIsInstance<CalculationStandardSet>().firstOrNull()
            return getStandardSetProgressions(
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
                setResults = setResults,
                displayResults = displayResults,
                isDeloadWeek =  true,
            )
        }
    }

    private fun getWeightRecommendation(
        lift: CalculationCustomWorkoutLift,
        set: CalculationCustomLiftSet,
        setData: SetResult?,
        lastCompletedStandardSetResult: SetResult?,
    ): Float? {
        return when {
            customSetExceededRepRangeTop(set, setData) ->
                getCalculatedWeightRecommendation(
                    increment = lift.incrementOverride,
                    repGoal = set.repRangeTop,
                    rpeTarget = set.rpeTarget,
                    result = setData!!
                )
            customSetMeetsCriterion(set, setData) -> incrementWeight(lift, setData!!)
            customSetShouldDecreaseWeight(set, setData) ->
                getCalculatedWeightRecommendation(
                    increment = lift.incrementOverride,
                    repGoal = set.repRangeBottom,
                    rpeTarget = set.rpeTarget,
                    result = setData!!)
            setData?.weight != null -> setData.weight
            lastCompletedStandardSetResult != null ->
                getCalculatedWeightRecommendation(lift.incrementOverride, set.repRangeBottom, set.rpeTarget, lastCompletedStandardSetResult)
            else -> null
        }
    }

    private fun getWeightRecommendation(
        lift: CalculationCustomWorkoutLift,
        set: CalculationMyoRepSet,
        setData: List<MyoRepSetResult>?,
        lastCompletedStandardSetResult: SetResult?,
    ): Float? {
        val activationSet = setData?.firstOrNull()
        return when {
            // Activation set exceeded top of rep range, recalculate
            customSetExceededRepRangeTop(set, activationSet) ->
                getCalculatedWeightRecommendation(
                    increment = lift.incrementOverride,
                    repGoal = set.repRangeTop,
                    rpeTarget = set.rpeTarget,
                    result = activationSet!!
                )

            // Met all criterion, increment weight
            customSetMeetsCriterion(set, setData) -> incrementWeight(lift, activationSet!!)

            // Recalculate weight if missed bottom of rep range
            activationSet != null && missedRepRangeBottom(
                repRangeBottom = set.repRangeBottom,
                rpeTarget = set.rpeTarget,
                completedReps = activationSet.reps,
                completedRpe = activationSet.rpe,
            ) -> getCalculatedWeightRecommendation(
                increment = lift.incrementOverride,
                repGoal = set.repRangeBottom,
                rpeTarget = set.rpeTarget,
                result = activationSet)

            // Activation set has a weight, return it
            activationSet != null -> activationSet.weight

            // Get a weight recommendation if there was no activation set but we have a previous
            // standard set weight recommendation to base it on
            lastCompletedStandardSetResult != null ->
                getCalculatedWeightRecommendation(
                    increment = lift.incrementOverride,
                    repGoal = set.repRangeBottom,
                    rpeTarget = set.rpeTarget,
                    result = lastCompletedStandardSetResult)

            // Nothing to do, return null
            else -> null
        }
    }

    private fun buildDropSetWeightRecommendationsMap(
        workoutLift: CalculationCustomWorkoutLift,
        setResults: List<SetResult>,
    ): Map<Long, Float> {
        if (workoutLift.customLiftSets.filterIsInstance<CalculationDropSet>().isEmpty()) {
            return mapOf()
        }

        val increment: Float = workoutLift.incrementOverride ?: SettingsManager.getSetting(
            INCREMENT_AMOUNT,
            DEFAULT_INCREMENT_AMOUNT
        )
        val dropSetGroups: Map<Int, List<SetResult>> =
            groupDropSetResultsByTopSetPosition(setResults)
        val sets = workoutLift.customLiftSets
        val setsByPosition = sets.associateBy { it.position }

        // Iterate top sets since dropSetGroups is by top set
        return sets.filterIsInstance<CalculationStandardSet>().flatMap { set ->
            dropSetGroups[set.position]?.let { results ->
                val droppedFromSetResult = results.find { it.setType == SetType.STANDARD }
                val allSetsMetCriterion = results.fastAll { result ->
                    val setForResult = setsByPosition[result.setPosition]!!
                    customSetMeetsCriterion(set = setForResult, result = result)
                }
                if (allSetsMetCriterion) {
                    getWeightRecommendation(
                        lift = workoutLift,
                        set = set,
                        setData = droppedFromSetResult,
                        lastCompletedStandardSetResult = null,
                    )?.let { topSetWeightRecommendation ->
                        results.fastMap { result ->
                            if (result.setType == SetType.DROP_SET) {
                                val dropSet = setsByPosition[result.setPosition]!! as CalculationDropSet
                                dropSet.id to
                                        getDropSetRecommendation(
                                            topSetWeightRecommendation = topSetWeightRecommendation,
                                            dropSetIndex = (result.setPosition - 1), // top set is index 0
                                            dropPercentage = dropSet.dropPercentage,
                                            roundingFactor = increment
                                        )
                            } else set.id to topSetWeightRecommendation
                        }
                    } ?: listOf()
                } else {
                    results.fastMap { result ->
                        val setForResult = setsByPosition[result.setPosition]!!
                        setForResult.id to
                                if (setForResult is CalculationDropSet) {
                                    getDropSetFailureWeight(
                                        incrementOverride = workoutLift.incrementOverride,
                                        repRangeBottom = setForResult.repRangeBottom,
                                        rpeTarget = setForResult.rpeTarget,
                                        dropPercentage = setForResult.dropPercentage,
                                        result = result,
                                        droppedFromSetResult = droppedFromSetResult,
                                    )!!
                                } else if (customSetShouldDecreaseWeight(set, result)) {
                                    getCalculatedWeightRecommendation(
                                        increment = workoutLift.incrementOverride,
                                        repGoal = set.repRangeBottom,
                                        rpeTarget = set.rpeTarget,
                                        result = result
                                    )
                                } else result.weight
                    }
                }
            } ?: listOf()
        }.associate { it.first to it.second }
    }


    private fun groupDropSetResultsByTopSetPosition(
        setResults: List<SetResult>,
    ): Map<Int, List<SetResult>> {
        val dropSetGroups: MutableMap<Int, List<SetResult>> = mutableMapOf()
        val dropSetsToAdd: MutableList<SetResult> = mutableListOf()

        setResults.fastForEachReversed { result ->
            when (result.setType) {
                SetType.DROP_SET -> dropSetsToAdd.add(result)
                SetType.STANDARD -> {
                    if (dropSetsToAdd.isNotEmpty()) {
                        dropSetsToAdd.add(result)
                        dropSetGroups[result.setPosition] = dropSetsToAdd.toList()
                        dropSetsToAdd.clear()
                    }
                }
                else -> {}
            }
        }

        return dropSetGroups.toMap()
    }

    private fun getDropSetRecommendation(
        topSetWeightRecommendation: Float,
        dropSetIndex: Int,
        dropPercentage: Float,
        roundingFactor: Float,
    ): Float {
        var currWeightRecommendation = topSetWeightRecommendation
        List(size = dropSetIndex + 1) {
            currWeightRecommendation *= (1 - dropPercentage)
        }

        return currWeightRecommendation.roundToNearestFactor(roundingFactor)
    }
}