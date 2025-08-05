package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class CreateWorkoutUseCase(
    private val workoutsRepository: WorkoutsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(program: Program, name: String) = transactionScope.execute {
        workoutsRepository.insert(
            Workout(
                programId = program.id,
                name = name,
                position = program.workouts.count(),
                lifts = listOf(),
            )
        )
    }
}