package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.Workout
import kotlinx.coroutines.flow.Flow

interface WorkoutsRepository : Repository<Workout, Long> {
    suspend fun updateName(id: Long, newName: String)
    fun getFlow(workoutId: Long): Flow<Workout?>
    fun getByMicrocyclePosition(
        programId: Long,
        microcyclePosition: Int,
    ): Flow<Workout?>
}
