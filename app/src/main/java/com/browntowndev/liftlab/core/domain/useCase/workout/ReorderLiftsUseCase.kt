package com.browntowndev.liftlab.core.domain.useCase.workout

import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class ReorderLiftsUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
) {
    suspend fun reorder(
        workout: LoggingWorkout,
        completedSets: List<SetResult>,
        newWorkoutLiftIndices: Map<Long, Int>,
    ) {
        val updatedLifts =
            workoutLiftsRepository.getForWorkout(workout.id)
                .map {
                    when (it) {
                        is StandardWorkoutLift -> it.copy(position = newWorkoutLiftIndices[it.id]!!)
                        is CustomWorkoutLift -> it.copy(position = newWorkoutLiftIndices[it.id]!!)
                        else -> throw Exception("${it::class.simpleName} is not defined.")
                    }
                }
        workoutLiftsRepository.updateMany(updatedLifts)

        val workoutLiftIdByLiftId = workout.lifts.associate { it.liftId to it.id }
        val updatedInProgressSetResults = completedSets.map { completedSet ->
                val workoutLiftIdOfCompletedSet = workoutLiftIdByLiftId[completedSet.liftId]
                when (completedSet) {
                    is StandardSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                    is MyoRepSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                    is LinearProgressionSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                    else -> throw Exception("${completedSet::class.simpleName} is not defined.")
                }
            }
        setResultsRepository.upsertMany(updatedInProgressSetResults)
    }
}