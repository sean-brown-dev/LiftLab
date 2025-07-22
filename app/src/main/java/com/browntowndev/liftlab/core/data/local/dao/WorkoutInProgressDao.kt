package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.browntowndev.liftlab.core.data.local.entities.WorkoutInProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutInProgressDao: BaseDao<WorkoutInProgressEntity> {
    @Query("DELETE FROM workoutsInProgress")
    suspend fun deleteAll()

    @Query("SELECT * FROM workoutsInProgress")
    suspend fun get(): WorkoutInProgressEntity?

    @Query("SELECT * FROM workoutsInProgress")
    fun getAllFlow(): Flow<List<WorkoutInProgressEntity>>

    @Query("SELECT * FROM workoutsInProgress WHERE workout_in_progress_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<WorkoutInProgressEntity>

    @Query("DELETE FROM workoutsInProgress")
    suspend fun delete()
}