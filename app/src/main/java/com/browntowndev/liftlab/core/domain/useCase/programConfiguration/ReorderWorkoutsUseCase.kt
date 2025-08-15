package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.ProgramDelta.WorkoutChange.WorkoutUpdate
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class ReorderWorkoutsUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workouts: List<Workout>, newOrders: Map<Long, Int>) = transactionScope.execute {
        val programId = workouts.fastMap { it.programId }.distinct().single()

        val delta = programDelta {
            workouts.fastForEach { workout ->
                val newOrder = newOrders[workout.id]
                if (newOrder != null) {
                    workout(
                        workoutId = workout.id,
                        workoutUpdate = WorkoutUpdate(position = Patch.Set(newOrder))
                    )
                } else throw IllegalArgumentException("New position not found for workout: ${workout.id}")
            }
        }

        programsRepository.applyDelta(programId, delta)
    }
}