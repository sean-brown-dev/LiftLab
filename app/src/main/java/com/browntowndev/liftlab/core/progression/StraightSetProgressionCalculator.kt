package com.browntowndev.liftlab.core.progression

import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgressionDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult

abstract class StraightSetProgressionCalculator: BaseProgressionCalculator() {
    override fun calculate(
        workoutLift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
    ): List<ProgressionDto>  {
        val sortedSetData = previousSetResults.sortedBy { it.setPosition }
        return calculateProgressions(workoutLift, sortedSetData)
    }

    protected open fun getFailureWeight(
        lift: GenericWorkoutLift,
        previousSetResults: List<SetResult>
    ): Float {
        return previousSetResults.firstOrNull()?.weight ?: 0f
    }

    protected abstract fun allSetsMetCriteria(
        lift: StandardWorkoutLiftDto,
        previousSetResults: List<SetResult>,
    ): Boolean

    protected abstract fun allSetsMetCriteria(
        lift: CustomWorkoutLiftDto,
        previousSetResults: List<SetResult>,
    ): Boolean

    private fun allSetsMetCriteria(
        lift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
    ): Boolean {
        return previousSetResults.isNotEmpty() &&
                when (lift) {
                    is StandardWorkoutLiftDto -> allSetsMetCriteria(lift, previousSetResults)
                    is CustomWorkoutLiftDto -> allSetsMetCriteria(lift, previousSetResults)
                    else -> throw Exception("${lift::class.simpleName} is not defined.")
                }
    }

    private fun calculateProgressions(
        lift: GenericWorkoutLift,
        previousSetResults: List<SetResult>,
    ): List<ProgressionDto> {
        if (previousSetResults.isEmpty()) return listOf()

        val topSet = previousSetResults.firstOrNull()
        val weightRecommendation = if(allSetsMetCriteria(lift, previousSetResults)) {
            incrementWeight(lift, topSet!!)
        } else getFailureWeight(lift = lift, previousSetResults = previousSetResults)

        val progression = List(lift.setCount) {
            ProgressionDto(
                setPosition = it,
                weightRecommendation = weightRecommendation
            )
        }

        return progression
    }
}