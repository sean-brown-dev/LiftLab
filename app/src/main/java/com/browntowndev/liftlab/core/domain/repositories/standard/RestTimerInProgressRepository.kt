package com.browntowndev.liftlab.core.domain.repositories.standard

import com.browntowndev.liftlab.core.domain.models.RestTimerInProgress
import kotlinx.coroutines.flow.Flow

interface RestTimerInProgressRepository : Repository<RestTimerInProgress, Long> {
    suspend fun insert(restTime: Long)
    suspend fun deleteAll()
}