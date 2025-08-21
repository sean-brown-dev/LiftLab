package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutInProgress
import kotlinx.coroutines.flow.Flow

interface WorkoutInProgressRepository {
    fun getFlow(): Flow<WorkoutInProgress?>

    suspend fun get(): WorkoutInProgress?

    suspend fun delete(): Int

    suspend fun upsert(model: WorkoutInProgress): Long
    
    suspend fun isWorkoutInProgress(workoutId: Long): Boolean
}
