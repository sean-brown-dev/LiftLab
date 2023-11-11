package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift

@Dao
interface WorkoutLiftsDao {
    @Insert
    suspend fun insert(workoutLift: WorkoutLift): Long

    @Transaction
    @Insert
    suspend fun insertAll(workoutLifts: List<WorkoutLift>): List<Long>

    @Transaction
    @Delete
    suspend fun delete(workoutLift: WorkoutLift)

    @Update
    suspend fun update(workoutLift: WorkoutLift)

    @Transaction
    @Update
    suspend fun updateMany(workoutLifts: List<WorkoutLift>)

    @Query("UPDATE workoutLifts SET liftId = :newLiftId WHERE workout_lift_id = :workoutLiftId")
    suspend fun updateLiftId(workoutLiftId: Long, newLiftId: Long)

    @Query("SELECT liftId FROM workoutLifts WHERE workoutId = :workoutId")
    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long>
}