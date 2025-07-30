package com.browntowndev.liftlab.core.domain.useCase.workoutBuilder

import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.StandardSet
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class AddSetUseCase(
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
) {
    suspend operator fun invoke(
        workoutLifts: List<GenericWorkoutLift>,
        workoutLiftId: Long,
    ) {
        val workoutLift = workoutLifts
            .filterIsInstance<CustomWorkoutLift>()
            .find { it.id == workoutLiftId }!!
            .let {
                // TODO: Get rid of setCount, just use lift.customLiftSets.size
                it.copy(setCount = it.setCount + 1)
            }
        workoutLiftsRepository.update(workoutLift)

        val newSet = StandardSet(
            workoutLiftId = workoutLift.id,
            position = workoutLift.customLiftSets.size,
            rpeTarget = 8f,
            repRangeBottom = 8,
            repRangeTop = 10
        )
        customLiftSetsRepository.insert(newSet)
    }
}