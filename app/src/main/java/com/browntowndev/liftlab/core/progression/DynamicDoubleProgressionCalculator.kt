package com.browntowndev.liftlab.core.progression

import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.SettingsManager
import com.browntowndev.liftlab.core.common.SettingsManager.SettingNames.INCREMENT_AMOUNT
import com.browntowndev.liftlab.core.common.enums.SetType
import com.browntowndev.liftlab.core.common.roundToNearestFactor
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

class DynamicDoubleProgressionCalculator: BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val sortedSetData = previousSetResults.sortedBy { it.setPosition }

        return when (workoutLift) {
            is StandardWorkoutLiftDto -> {
                getStandardSetProgressions(workoutLift, sortedSetData, isDeloadWeek)
            }

            is CustomWorkoutLiftDto -> {
                getCustomSetProgressions(workoutLift, sortedSetData, isDeloadWeek)
            }

            else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
        }
    }

    private fun getStandardSetProgressions(
        workoutLift: StandardWorkoutLiftDto,
        sortedSetData: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<LoggingStandardSetDto> {
        val resultsMap = sortedSetData.associateBy { it.setPosition }
        return List(workoutLift.setCount) { setPosition ->
            val result = resultsMap[setPosition]
            LoggingStandardSetDto(
                position = setPosition,
                rpeTarget = workoutLift.rpeTarget,
                repRangeBottom = workoutLift.repRangeBottom,
                repRangeTop = workoutLift.repRangeTop,
                previousSetResultLabel = getPreviousSetResultLabel(result),
                repRangePlaceholder = if (!isDeloadWeek) {
                    "${workoutLift.repRangeBottom}-${workoutLift.repRangeTop}"
                } else workoutLift.repRangeBottom.toString(),
                weightRecommendation = if (setMetCriterion(result, workoutLift)) {
                    incrementWeight(workoutLift, result!!)
                } else if (shouldDecreaseWeight(result, workoutLift)) {
                    decreaseWeight(
                        workoutLift.incrementOverride,
                        workoutLift.repRangeBottom,
                        workoutLift.rpeTarget,
                        result!!
                    )
                } else result?.weight
            )
        }
    }

    private fun setMetCriterion(result: SetResult?, goals: StandardWorkoutLiftDto): Boolean {
        return result != null &&
                result.reps >= goals.repRangeTop &&
                result.rpe <= goals.rpeTarget
    }

    private fun getCustomSetProgressions(
        workoutLift: CustomWorkoutLiftDto,
        sortedSetData: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        val standardSetResults = sortedSetData
            .filterNot { it is MyoRepSetResultDto || it.setType == SetType.DROP_SET }
            .associateBy { it.setPosition }

        val dropSetResults = buildDropSetWeightRecommendationsMap(
            workoutLift = workoutLift,
            setResults = sortedSetData,
        )

        val myoRepSetResults = sortedSetData
            .filterIsInstance<MyoRepSetResultDto>()
            .groupBy { it.setPosition }

        return workoutLift.customLiftSets.flatMap { set ->
            when (set) {
                is MyoRepSetDto -> {
                    val allMyoRepSets = myoRepSetResults[set.position]
                    val weightRecommendation =
                        getWeightRecommendation(workoutLift, set, myoRepSetResults[set.position])

                    (allMyoRepSets?.fastMap {
                        LoggingMyoRepSetDto(
                            position = set.position,
                            myoRepSetPosition = it.myoRepSetPosition,
                            rpeTarget = set.rpeTarget,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            previousSetResultLabel = getPreviousSetResultLabel(result = it),
                            weightRecommendation = weightRecommendation,
                            repRangePlaceholder = if (!isDeloadWeek && it.myoRepSetPosition == null) {
                                "${set.repRangeBottom}-${set.repRangeTop}"
                            } else if (!isDeloadWeek && set.repFloor != null) {
                                ">${set.repFloor}"
                            } else if (!isDeloadWeek) {
                                "—"
                            } else {
                                set.repRangeBottom.toString()
                            },
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
                                    maxSets = set.maxSets,
                                    repFloor = set.repFloor,
                                    previousSetResultLabel = getPreviousSetResultLabel(result = null),
                                    repRangePlaceholder = if (!isDeloadWeek) {
                                        "${set.repRangeBottom}-${set.repRangeTop}"
                                    } else {
                                        set.repRangeBottom.toString()
                                    },
                                    weightRecommendation = weightRecommendation,
                                )
                            )
                        }
                    }
                }

                is DropSetDto -> {
                    val result = standardSetResults[set.position]
                    listOf(
                        LoggingDropSetDto(
                            dropPercentage = set.dropPercentage,
                            position = set.position,
                            rpeTarget = set.rpeTarget,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            previousSetResultLabel = getPreviousSetResultLabel(result),
                            repRangePlaceholder = if (!isDeloadWeek) {
                                "${set.repRangeBottom}-${set.repRangeTop}"
                            } else set.repRangeBottom.toString(),
                            weightRecommendation = dropSetResults[set.id],
                        )
                    )
                }

                is StandardSetDto -> {
                    val result = standardSetResults[set.position]
                    listOf(
                        LoggingStandardSetDto(
                            position = set.position,
                            rpeTarget = set.rpeTarget,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            previousSetResultLabel = getPreviousSetResultLabel(result),
                            repRangePlaceholder = if (!isDeloadWeek) {
                                "${set.repRangeBottom}-${set.repRangeTop}"
                            } else set.repRangeBottom.toString(),
                            weightRecommendation = dropSetResults[set.id]
                                ?: getWeightRecommendation(
                                    lift = workoutLift,
                                    set = set,
                                    setData = result,
                                )
                        )
                    )
                }

                else -> throw Exception("${set::class.simpleName} is not defined.")
            }
        }
    }

    private fun getWeightRecommendation(
        lift: CustomWorkoutLiftDto,
        set: GenericLiftSet,
        setData: SetResult?,
    ): Float? {
        return if (customSetMeetsCriterion(set, setData)) {
            incrementWeight(lift, setData!!)
        } else if (customSetShouldDecreaseWeight(set, setData)) {
            decreaseWeight(lift.incrementOverride, set.repRangeBottom, set.rpeTarget, setData!!)
        } else setData?.weight
    }

    private fun getWeightRecommendation(
        lift: CustomWorkoutLiftDto,
        set: MyoRepSetDto,
        setData: List<MyoRepSetResultDto>?,
    ): Float? {
        val activationSet = setData?.firstOrNull()
        return if (customSetMeetsCriterion(set, setData))
            incrementWeight(lift, activationSet!!)
        else
            activationSet?.weight
    }

    private fun buildDropSetWeightRecommendationsMap(
        workoutLift: CustomWorkoutLiftDto,
        setResults: List<SetResult>,
    ): Map<Long, Float> {
        if (workoutLift.customLiftSets.filterIsInstance<DropSetDto>().isEmpty()) {
            return mapOf()
        }

        val increment: Float = workoutLift.incrementOverride ?: SettingsManager.getSetting(
            INCREMENT_AMOUNT,
            5f
        )
        val dropSetGroups: Map<Int, List<SetResult>> =
            groupDropSetResultsByTopSetPosition(setResults)
        val sets = workoutLift.customLiftSets
        val setsByPosition = sets.associateBy { it.position }

        // Iterate top sets since dropSetGroups is by top set
        return sets.filterIsInstance<StandardSetDto>().flatMap { set ->
            dropSetGroups[set.position]?.let { results ->
                val droppedFromSetResult = results.find { it.setType == SetType.STANDARD }
                val allSetsMetCriterion = results.fastAll { result ->
                    val setForResult = setsByPosition[result.setPosition]!!
                    customSetMeetsCriterion(set = setForResult, previousSet = result)
                }
                if (allSetsMetCriterion) {
                    getWeightRecommendation(
                        lift = workoutLift,
                        set = set,
                        setData = droppedFromSetResult,
                    )?.let { topSetWeightRecommendation ->
                        results.fastMap { result ->
                            if (result.setType == SetType.DROP_SET) {
                                val dropSet = setsByPosition[result.setPosition]!! as DropSetDto
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
                                if (setForResult is DropSetDto) {
                                    getDropSetFailureWeight(
                                        incrementOverride = workoutLift.incrementOverride,
                                        repRangeBottom = setForResult.repRangeBottom,
                                        rpeTarget = setForResult.rpeTarget,
                                        dropPercentage = setForResult.dropPercentage,
                                        result = result,
                                        droppedFromSetResult = droppedFromSetResult,
                                    )!!
                                } else if (customSetShouldDecreaseWeight(set, result)) {
                                    decreaseWeight(
                                        incrementOverride = workoutLift.incrementOverride,
                                        repRangeBottom = set.repRangeBottom,
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