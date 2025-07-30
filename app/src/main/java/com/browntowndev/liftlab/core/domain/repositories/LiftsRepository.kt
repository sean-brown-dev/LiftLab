package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.metadata.LiftMetadata
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface LiftsRepository : Repository<Lift, Long> {
    suspend fun updateRestTime(id: Long, enabled: Boolean, newRestTime: Duration?)
    suspend fun updateIncrementOverride(id: Long, newIncrement: Float?)
    suspend fun updateNote(id: Long, note: String?)
    fun getManyMetadataFlow(ids: List<Long>): Flow<List<LiftMetadata>>
}