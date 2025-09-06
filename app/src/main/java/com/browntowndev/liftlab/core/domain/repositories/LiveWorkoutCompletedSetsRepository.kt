package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult

interface LiveWorkoutCompletedSetsRepository : Repository<SetResult, Long> {
    suspend fun deleteAll()
    suspend fun getAllForLiftAtPosition(liftId: Long, liftPosition: Int, position: Int): List<SetResult>
    suspend fun changeFromLiftsToNewLift(newLiftId: Long, existingLiftIds: List<Long>)
}
