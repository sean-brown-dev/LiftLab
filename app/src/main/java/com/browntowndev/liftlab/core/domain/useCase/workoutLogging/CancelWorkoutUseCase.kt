package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository

class CancelWorkoutUseCase(
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val setResultsRepository: PreviousSetResultsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programMetadata: ActiveProgramMetadata,
        workout: LoggingWorkout,
    ) = transactionScope.execute {
        // Remove the workoutEntity from in progress
        workoutInProgressRepository.deleteAll()

        // Delete all set results from the workoutEntity
        setResultsRepository.deleteAllForWorkout(
            workoutId = workout.id,
            mesoCycle = programMetadata.currentMesocycle,
            microCycle = programMetadata.currentMicrocycle,
        )
    }
}