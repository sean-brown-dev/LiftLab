package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class CreateWorkoutUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(program: Program, name: String) = transactionScope.execute {
        val delta = programDelta {
            workout(
                insertWorkout = Workout(
                    programId = program.id,
                    name = name,
                    position = 0,
                    lifts = emptyList()
                )
            )
        }

        programsRepository.applyDelta(program.id, delta)
    }
}