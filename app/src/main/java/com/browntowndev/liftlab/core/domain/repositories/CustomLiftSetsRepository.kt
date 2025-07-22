package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.data.remote.sync.SyncQueueEntry

interface CustomLiftSetsRepository : Repository<GenericLiftSet, Long> {
    suspend fun deleteAllForLift(workoutLiftId: Long)
    suspend fun deleteByPosition(workoutLiftId: Long, position: Int)
    suspend fun updateManyAndGetSyncQueueEntry(sets: List<GenericLiftSet>): SyncQueueEntry?
}