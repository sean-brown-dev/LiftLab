package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.RestTimerInProgress
import kotlinx.coroutines.flow.Flow

interface RestTimerInProgressRepository : Repository<RestTimerInProgress, Long> {
    suspend fun get(): RestTimerInProgress?
    fun getFlow(): Flow<RestTimerInProgress?>
    suspend fun insert(restTime: Long)
    suspend fun deleteAll()
}