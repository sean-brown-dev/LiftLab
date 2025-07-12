package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutLiftsDao: BaseDao<WorkoutLift> {
    @Query("SELECT * FROM workoutLifts")
    suspend fun getAll(): List<WorkoutLift>

    @Query("DELETE FROM workoutLifts")
    suspend fun deleteAll()

    @Query("UPDATE workoutLifts SET liftId = :newLiftId WHERE workout_lift_id = :workoutLiftId")
    suspend fun updateLiftId(workoutLiftId: Long, newLiftId: Long)

    @Query("SELECT liftId FROM workoutLifts WHERE workoutId = :workoutId")
    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long>

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workoutId = :workoutId")
    suspend fun getForWorkout(workoutId: Long): List<WorkoutLiftWithRelationships>
}