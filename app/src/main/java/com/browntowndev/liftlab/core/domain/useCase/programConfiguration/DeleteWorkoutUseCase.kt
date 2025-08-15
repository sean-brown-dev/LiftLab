package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMaxOfOrNull
import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.common.valueOrDefault
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.delta.programDeltaSuspend
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class DeleteWorkoutUseCase(
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workout: Workout) = transactionScope.execute {
        val delta = programDeltaSuspend {
            removeWorkouts(workout.id)
            workoutsRepository.getAllForProgramWithoutLiftsPopulated(workout.programId)
                .filterNot { it.id == workout.id }
                .sortedBy { it.position }
                .fastForEachIndexed { index, workoutEntity ->
                    workout(workoutId = workoutEntity.id, position = Patch.Set(index))
                }
        }
        programsRepository.applyDelta(workout.programId, delta)

        // If current microcycle position is now greater than the number of workouts
        // set it to the last workoutEntity index
        val lastWorkoutPosition = delta.workouts.fastMaxOfOrNull { it.workoutUpdate?.position?.valueOrDefault(0) ?: 0 } ?: 0
        programsRepository.getActive()?.let { program ->
            if (program.currentMicrocyclePosition > lastWorkoutPosition) {
                val microcyclePositionDelta = programDelta {
                    updateProgram(currentMicrocyclePosition = Patch.Set(lastWorkoutPosition))
                }
                programsRepository.applyDelta(program.id, microcyclePositionDelta)
            }
        }
    }
}