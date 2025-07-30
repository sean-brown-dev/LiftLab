package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.domain.extensions.convertToCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.extensions.convertToStandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class ConvertWorkoutLiftTypeUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
) {
    suspend operator fun invoke(workoutLiftToConvert: GenericWorkoutLift, enableCustomSets: Boolean) {
        if (enableCustomSets) {
            val standardWorkoutLift = workoutLiftToConvert as? StandardWorkoutLift
                ?: throw Exception("Lift already has custom lift sets.")

            val customWorkoutLift = standardWorkoutLift.convertToCustomWorkoutLift()
            workoutLiftsRepository.update(customWorkoutLift)
            customLiftSetsRepository.insertMany(customWorkoutLift.customLiftSets)
        } else {
            val customWorkoutLift = workoutLiftToConvert as? CustomWorkoutLift
                ?: throw Exception("Lift does not have custom lift sets to remove.")

            val standardWorkoutLift = customWorkoutLift.convertToStandardWorkoutLift()
            workoutLiftsRepository.update(standardWorkoutLift)
            customLiftSetsRepository.deleteAllForLift(workoutLiftToConvert.id)
        }
    }
}