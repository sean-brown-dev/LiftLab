package com.browntowndev.liftlab.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.dtos.ProgramDto
import com.browntowndev.liftlab.core.data.entities.Workout

@Dao
interface WorkoutsDao {
    @Insert
    suspend fun insert(workout: Workout): Long

    @Delete
    suspend fun delete(workout: Workout)

    @Query("UPDATE workouts SET name = :newName WHERE workout_id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id = :id")
    suspend fun get(id: Long) : ProgramDto.WorkoutDto
}