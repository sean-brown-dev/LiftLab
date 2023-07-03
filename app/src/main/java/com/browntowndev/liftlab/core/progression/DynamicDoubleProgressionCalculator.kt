package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgressionDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetResultDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericCustomLiftSet
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

class DynamicDoubleProgressionCalculator: BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
    ): List<ProgressionDto> {
        val sortedSetData = previousSetResults.sortedBy { it.setPosition }

        return when (workoutLift) {
            is StandardWorkoutLiftDto -> {
                getStandardSetProgressions(workoutLift, sortedSetData)
            }
            is CustomWorkoutLiftDto -> {
                getCustomSetProgressions(workoutLift, sortedSetData)
            }
            else -> throw Exception("${workoutLift::class.simpleName} is not defined.")
        }
    }

    private fun getStandardSetProgressions(
        workoutLiftDto: StandardWorkoutLiftDto,
        sortedSetData: List<SetResult>,
    ): List<ProgressionDto> {
        return sortedSetData.map {
            ProgressionDto(
                setPosition = it.setPosition,
                weightRecommendation =
                    if (it.reps == workoutLiftDto.repRangeTop &&
                        it.rpe == workoutLiftDto.rpeTarget) {
                        incrementWeight(workoutLiftDto, it)
                    } else it.weight
            )
        }
    }

    private fun getCustomSetProgressions(
        lift: CustomWorkoutLiftDto,
        sortedSetData: List<SetResult>,
    ): List<ProgressionDto> {
        return lift.customLiftSets.map { set ->
            when (set) {
                is MyoRepSetDto -> {
                    val groupedSetData = sortedSetData.filterIsInstance<MyoRepSetResultDto>().groupBy { it.setPosition }
                    getSetProgression(lift, set, groupedSetData[set.position])
                }
                else -> {
                    val groupedSetData = sortedSetData.filterIsInstance<StandardSetResultDto>().groupBy { it.setPosition }
                    getSetProgression(lift, set, groupedSetData[set.position]?.firstOrNull())
                }
            }
        }
    }

    private fun getSetProgression(
        lift: CustomWorkoutLiftDto,
        set: GenericCustomLiftSet,
        setData: StandardSetResultDto?,
    ) : ProgressionDto {
        return ProgressionDto(
            setPosition = set.position,
            weightRecommendation = if (customSetMeetsCriterion(set, setData)) incrementWeight(lift, setData!!)
                else setData?.weight ?: 0f
        )
    }

    private fun getSetProgression(
        lift: CustomWorkoutLiftDto,
        set: MyoRepSetDto,
        setData: List<MyoRepSetResultDto>?,
    ) : ProgressionDto {
        val activationSet = setData?.firstOrNull()

        return ProgressionDto(
            setPosition = set.position,
            weightRecommendation =
                if (customSetMeetsCriterion(set, setData))
                    incrementWeight(lift, activationSet!!)
                else
                    activationSet?.weight ?: 0f
        )
    }
}