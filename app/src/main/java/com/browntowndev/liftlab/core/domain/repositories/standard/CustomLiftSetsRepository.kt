package com.browntowndev.liftlab.core.domain.repositories.standard

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry

interface CustomLiftSetsRepository : Repository<GenericLiftSet, Long> {
    suspend fun deleteAllForLift(workoutLiftId: Long)
    suspend fun deleteByPosition(workoutLiftId: Long, position: Int)
    suspend fun updateManyAndGetSyncQueueEntry(sets: List<GenericLiftSet>): SyncQueueEntry?
}