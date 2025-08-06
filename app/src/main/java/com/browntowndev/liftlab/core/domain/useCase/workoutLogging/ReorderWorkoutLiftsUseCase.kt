package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class ReorderWorkoutLiftsUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val setResultsRepository: LiveWorkoutCompletedSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        workout: LoggingWorkout,
        completedSets: List<SetResult>,
        newWorkoutLiftIndices: Map<Long, Int>,
    ) = transactionScope.execute {
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

        if (completedSets.isEmpty()) return@execute
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