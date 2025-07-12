package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.entities.WorkoutInProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutInProgressDao: BaseDao<WorkoutInProgress> {
    @Query("DELETE FROM workoutsInProgress")
    suspend fun deleteAll()

    @Query("SELECT * FROM workoutsInProgress")
    suspend fun get(): WorkoutInProgress?

    @Query("DELETE FROM workoutsInProgress")
    suspend fun delete()
}