package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult

interface LiveWorkoutCompletedSetsRepository : Repository<SetResult, Long> {
    suspend fun deleteAll()
}
