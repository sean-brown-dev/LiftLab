package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.ui.mapping.toDomainModel
import com.browntowndev.liftlab.ui.models.workout.WorkoutInProgressUiModel

class StartWorkoutUseCase(
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutId: Long) = transactionScope.execute  {
        val inProgressWorkout = WorkoutInProgressUiModel(
            startTime = getCurrentDate(),
        )
        workoutInProgressRepository.upsert(
            inProgressWorkout.toDomainModel(
                workoutId
            )
        )
    }
}