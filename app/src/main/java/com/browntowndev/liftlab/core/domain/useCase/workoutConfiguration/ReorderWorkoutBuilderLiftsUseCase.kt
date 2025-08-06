package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class ReorderWorkoutBuilderLiftsUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val programsRepository: ProgramsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        workoutId: Long,
        workoutLifts: List<GenericWorkoutLift>,
        newWorkoutLiftIndices: Map<Long, Int>,
    ) = transactionScope.execute {
        val updatedWorkoutLifts = workoutLifts.fastMap { lift ->
            when(lift) {
                is StandardWorkoutLift -> lift.copy(position = newWorkoutLiftIndices[lift.id]!!)
                is CustomWorkoutLift -> lift.copy(position = newWorkoutLiftIndices[lift.id]!!)
                else -> throw Exception("${lift::class.simpleName} is not defined.")
            }
        }
        workoutLiftsRepository.updateMany(updatedWorkoutLifts)

        if (workoutInProgressRepository.isWorkoutInProgress(workoutId)) {
            programsRepository.getActive()?.let { programMetadata ->
                val workoutLiftsByLiftId = updatedWorkoutLifts.associateBy { it.liftId }
                val updatedInProgressSetResults = liveWorkoutCompletedSetsRepository.getAll()
                    .map { completedSet ->
                        val workoutLiftOfCompletedSet = workoutLiftsByLiftId[completedSet.liftId]!!
                        when (completedSet) {
                            is StandardSetResult -> completedSet.copy(liftPosition = workoutLiftOfCompletedSet.position)
                            is MyoRepSetResult -> completedSet.copy(liftPosition = workoutLiftOfCompletedSet.position)
                            is LinearProgressionSetResult -> completedSet.copy(liftPosition = workoutLiftOfCompletedSet.position)
                            else -> throw Exception("${completedSet::class.simpleName} is not defined.")
                        }
                    }

                liveWorkoutCompletedSetsRepository.upsertMany(updatedInProgressSetResults)
            }
        }
    }
}