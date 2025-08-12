package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet

interface CustomLiftSetsRepository : Repository<GenericLiftSet, Long> {
    suspend fun deleteAllForLift(workoutLiftId: Long)
    suspend fun deleteByPosition(workoutLiftId: Long, position: Int): Int
    fun deleteByProgramId(programId: Long)
}