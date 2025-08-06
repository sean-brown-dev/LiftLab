package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class DeleteWorkoutUseCase(
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workout: Workout) = transactionScope.execute {
        workoutsRepository.delete(workout)

        // Delete any workoutLifts for this workout
        val workoutLiftsToDelete = workoutLiftsRepository.getForWorkout(workout.id)
        workoutLiftsRepository.deleteMany(workoutLiftsToDelete)

        // Delete any custom lift sets for this workout
        workoutLiftsToDelete.fastForEach {
            customLiftSetsRepository.deleteAllForLift(workout.id)
        }

        // Delete any workoutInProgress records for this workout
        workoutInProgressRepository.getAll().fastFirstOrNull { it.workoutId == workout.id }?.let {
            workoutInProgressRepository.delete(it)
        }

        // Delete all the previous set results for this workout
        liveWorkoutCompletedSetsRepository.deleteAll()

        // Update workout positions for other workouts on the same program
        val workoutsWithNewPositions = workoutsRepository.getAllForProgramWithoutLiftsPopulated(workout.programId)
            .sortedBy { it.position }
            .fastMapIndexed { index, workoutEntity ->
                workoutEntity.copy(position = index)
            }
        workoutsRepository.updateMany(workoutsWithNewPositions)

        // If current microcycle position is now greater than the number of workouts
        // set it to the last workoutEntity index
        programsRepository.getActive()?.let { program ->
            if (program.currentMicrocyclePosition > workoutsWithNewPositions.lastIndex) {
                programsRepository.updateMesoAndMicroCycle(
                    id = program.id,
                    mesoCycle = program.currentMesocycle,
                    microCycle = program.currentMicrocycle,
                    microCyclePosition = workoutsWithNewPositions.lastIndex
                )
            }
        }
    }
}