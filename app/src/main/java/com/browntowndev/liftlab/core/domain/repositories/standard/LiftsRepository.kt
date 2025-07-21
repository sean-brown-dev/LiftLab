package com.browntowndev.liftlab.core.domain.repositories.standard

import com.browntowndev.liftlab.core.domain.models.Lift
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface LiftsRepository : Repository<Lift, Long> {
    fun getAllFlow(): Flow<List<Lift>>
    suspend fun updateRestTime(id: Long, enabled: Boolean, newRestTime: Duration?)
    suspend fun updateIncrementOverride(id: Long, newIncrement: Float?)
    suspend fun updateNote(id: Long, note: String?)
}