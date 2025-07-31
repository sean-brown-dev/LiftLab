package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class CreateWorkoutUseCase(
    private val workoutsRepository: WorkoutsRepository,
) {
    suspend operator fun invoke(program: Program, name: String) {
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