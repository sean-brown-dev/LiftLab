package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.workoutLogging.RestTimerInProgress
import kotlinx.coroutines.flow.Flow

interface RestTimerInProgressRepository {
    suspend fun get(): RestTimerInProgress?
    fun getFlow(): Flow<RestTimerInProgress?>
    suspend fun upsert(startTimeInMillis: Long, restTimeInMillis: Long)
    suspend fun delete()
}