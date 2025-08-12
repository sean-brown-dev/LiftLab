package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import android.util.Log
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics

class DeleteCustomLiftSetByPositionUseCase(
    private val customSetsRepository: CustomLiftSetsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutLiftId: Long, position: Int) = transactionScope.execute {
        val deleteCount = customSetsRepository.deleteByPosition(workoutLiftId, position)

        if (deleteCount > 0) {
            if (deleteCount > 1) {
                Log.w("DeleteCustomLiftSetByPositionUseCase", "deleted more than one set: $deleteCount")
                FirebaseCrashlytics.getInstance().log("deleted more than one set: $deleteCount")
            }

            val workoutLift = workoutLiftsRepository.getById(workoutLiftId) ?: return@execute
            val newSetCount = (workoutLift.setCount - deleteCount).coerceAtLeast(0)
            val updatedWorkoutLift = when (workoutLift) {
                is StandardWorkoutLift -> workoutLift.copy(setCount = newSetCount)
                is CustomWorkoutLift -> workoutLift.copy(setCount = newSetCount)
                else -> throw Exception("Unknown workout lift type")
            }
            workoutLiftsRepository.update(updatedWorkoutLift)
        }
    }
}