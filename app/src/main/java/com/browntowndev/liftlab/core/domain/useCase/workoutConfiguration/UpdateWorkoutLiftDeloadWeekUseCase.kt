package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.extensions.getRecalculatedStepSizeForLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class UpdateWorkoutLiftDeloadWeekUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
) {
    suspend operator fun invoke(workoutLift: GenericWorkoutLift, deloadWeek: Int?, programDeloadWeek: Int) {
        val updatedWorkoutLift = when (workoutLift) {
            is StandardWorkoutLift -> workoutLift.copy(
                deloadWeek = deloadWeek,
                stepSize = workoutLift.getRecalculatedStepSizeForLift(
                    deloadToUseInsteadOfLiftLevel = deloadWeek ?: programDeloadWeek,
                )
            )

            is CustomWorkoutLift -> workoutLift.copy(deloadWeek = deloadWeek)
            else -> throw Exception("${workoutLift::class.simpleName} not recognized.")
        }
        workoutLiftsRepository.update(updatedWorkoutLift)
    }
}