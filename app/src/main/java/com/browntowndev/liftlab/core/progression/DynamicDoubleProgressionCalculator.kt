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
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
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
                setPosition = setPosition,
                rpeTarget = workoutLift.rpeTarget,
                repRangeBottom = workoutLift.repRangeBottom,
                repRangeTop = workoutLift.repRangeTop,
                previousSetResultLabel =
                if (result != null) {
                    "${result.weight}x${result.reps} @${result.rpe}"
                }
                else "—",
                repRangePlaceholder = if (!isDeloadWeek) {
                    "${workoutLift.repRangeBottom}-${workoutLift.repRangeTop}"
                } else workoutLift.repRangeBottom.toString(),
                weightRecommendation =
                if ((result?.reps ?: 0) >= workoutLift.repRangeTop &&
                    result?.rpe == workoutLift.rpeTarget
                ) {
                    incrementWeight(workoutLift, result)
                } else result?.weight
            )
        }
    }

    private fun getCustomSetProgressions(
        workoutLift: CustomWorkoutLiftDto,
        sortedSetData: List<SetResult>,
        isDeloadWeek: Boolean,
    ): List<GenericLoggingSet> {
        return workoutLift.customLiftSets.flatMap { set ->
            when (set) {
                is MyoRepSetDto -> {
                    val groupedSetData = sortedSetData.filterIsInstance<MyoRepSetResultDto>()
                        .groupBy { it.setPosition }
                    val allMyoRepSets = groupedSetData[set.setPosition]
                    val activationSet = allMyoRepSets?.first { it.myoRepSetPosition == null }
                    val myoRepSets = allMyoRepSets?.filter { it.myoRepSetPosition != null }
                    val weightRecommendation =
                        getWeightRecommendation(workoutLift, set, groupedSetData[set.setPosition])

                    (myoRepSets?.fastMap {
                        LoggingMyoRepSetDto(
                            setPosition = set.setPosition,
                            rpeTarget = set.rpeTarget,
                            repRangeBottom = set.repRangeBottom,
                            repRangeTop = set.repRangeTop,
                            previousSetResultLabel = "${it.weight}x${it.reps} @${it.rpe}",
                            weightRecommendation = weightRecommendation,
                            myoRepSetPosition = it.myoRepSetPosition,
                            repRangePlaceholder = if (!isDeloadWeek) {
                                "${set.repRangeBottom}-${set.repRangeTop}"
                            } else set.repRangeBottom.toString(),
                        )
                    }?.toMutableList() ?: mutableListOf()).apply {
                        add(
                            LoggingMyoRepSetDto(
                                setPosition = set.setPosition,
                                rpeTarget = set.rpeTarget,
                                repRangeBottom = set.repRangeBottom,
                                repRangeTop = set.repRangeTop,
                                setMatching = set.setMatching,
                                maxSets = set.maxSets,
                                repFloor = set.repFloor,
                                previousSetResultLabel =
                                if (activationSet != null) {
                                    "${activationSet.weight}x${activationSet.reps} @${activationSet.rpe}"
                                } else "—",
                                repRangePlaceholder = if (!isDeloadWeek) {
                                    "${set.repRangeBottom}-${set.repRangeTop}"
                                } else set.repRangeBottom.toString(),
                                weightRecommendation = weightRecommendation,
                            )
                        )
                    }
                }

                is DropSetDto -> {
                    val groupedSetData = sortedSetData
                        .filterIsInstance<StandardSetResultDto>()
                        .groupBy { it.setPosition }
                    val result = groupedSetData[set.setPosition]?.first()
                    listOf(
                        LoggingDropSetDto(
                            dropPercentage = set.dropPercentage,
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
                            weightRecommendation = getWeightRecommendation(
                                workoutLift, set, groupedSetData[set.setPosition]?.firstOrNull()
                            )
                        )
                    )
                }

                is StandardSetDto -> {
                    val groupedSetData = sortedSetData
                        .filterIsInstance<StandardSetResultDto>()
                        .groupBy { it.setPosition }
                    val result = groupedSetData[set.setPosition]?.first()
                    listOf(
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
                            weightRecommendation = getWeightRecommendation(
                                workoutLift, set, groupedSetData[set.setPosition]?.firstOrNull()
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
        setData: StandardSetResultDto?,
    ) : Float? {
        return if (customSetMeetsCriterion(set, setData)) {
            incrementWeight(lift, setData!!)
        }
        else setData?.weight
    }

    private fun getWeightRecommendation(
        lift: CustomWorkoutLiftDto,
        set: MyoRepSetDto,
        setData: List<MyoRepSetResultDto>?,
    ) : Float? {
        val activationSet = setData?.firstOrNull()
        return if (customSetMeetsCriterion(set, setData))
            incrementWeight(lift, activationSet!!)
        else
            activationSet?.weight
    }
}