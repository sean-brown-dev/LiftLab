package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import android.util.Log
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository

class DeleteSetResultByIdUseCase(
    private val liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(id: Long): Int  = transactionScope.execute {
        val requestedToDelete = liveWorkoutCompletedSetsRepository.getById(id) ?: return@execute 0
        if (requestedToDelete !is MyoRepSetResult) {
            liveWorkoutCompletedSetsRepository.delete(requestedToDelete)
        } else {
            val sequenceToDelete = liveWorkoutCompletedSetsRepository.getAllForLiftAtPosition(
                liftId = requestedToDelete.liftId,
                liftPosition = requestedToDelete.liftPosition,
                requestedToDelete.setPosition
            ).filter { myoRepInSequence ->
                val myoRepPosition = (myoRepInSequence as? MyoRepSetResult)?.myoRepSetPosition ?: -1
                myoRepPosition >= (requestedToDelete.myoRepSetPosition ?: -1)
            }

            Log.d("DeleteSetResultByIdUseCase", "sequenceToDelete: $sequenceToDelete")
            liveWorkoutCompletedSetsRepository.deleteMany(sequenceToDelete)
        }
    }
}