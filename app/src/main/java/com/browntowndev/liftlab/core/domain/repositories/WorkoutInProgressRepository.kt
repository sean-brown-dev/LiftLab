package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.WorkoutInProgress
import kotlinx.coroutines.flow.Flow

interface WorkoutInProgressRepository : Repository<WorkoutInProgress, Long> {
    suspend fun getWithoutCompletedSets(): WorkoutInProgress?
    fun getFlow(mesoCycle: Int, microCycle: Int): Flow<WorkoutInProgress?>
    suspend fun deleteAll(): Int
    
    suspend fun isWorkoutInProgress(workoutId: Long): Boolean
}
