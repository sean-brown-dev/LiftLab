package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift

@Dao
interface WorkoutLiftsDao {
    @Insert
    suspend fun insert(workoutLift: WorkoutLift): Long

    @Delete
    suspend fun delete(workoutLift: WorkoutLift)

    @Query("UPDATE workoutLifts SET liftId = :liftId WHERE workout_lift_id = :id")
    suspend fun updateLift(id: Long, liftId: Long)

    @Query("UPDATE workoutLifts SET position = :newPosition WHERE workout_lift_id = :id")
    suspend fun updatePosition(id: Long, newPosition: Long)

    @Query("UPDATE workoutLifts SET rpeTarget = :newRpe WHERE workout_lift_id = :id")
    suspend fun updateTopSetRpe(id: Long, newRpe: Long)

    @Query("UPDATE workoutLifts SET progressionScheme = :newProgressionScheme WHERE workout_lift_id = :id")
    suspend fun updateProgressionScheme(id: Long, newProgressionScheme: ProgressionScheme)
}