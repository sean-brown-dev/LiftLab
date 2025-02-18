package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.browntowndev.liftlab.core.persistence.dtos.queryable.ProgramWithRelationships
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutsDao {
    @Insert(entity = Workout::class)
    suspend fun insert(workout: Workout): Long

    @Delete(entity = Workout::class)
    suspend fun delete(workout: Workout)

    @Transaction
    @Update(entity = Workout::class)
    suspend fun updateMany(workout: List<Workout>)

    @Transaction
    @Update(entity = Workout::class)
    suspend fun update(workout: Workout)

    @Query("DELETE FROM workouts WHERE workout_id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE workouts SET name = :newName WHERE workout_id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id = :id")
    suspend fun get(id: Long) : WorkoutWithRelationships?

    @Query("SELECT MAX(position) FROM workouts WHERE programId = :programId")
    suspend fun getFinalPosition(programId: Long): Int

    @Transaction
    @Query("SELECT * FROM workouts " +
            "WHERE position = :microcyclePosition AND " +
            "programId = :programId")
    fun getByMicrocyclePosition(programId: Long, microcyclePosition: Int): Flow<WorkoutWithRelationships?>

    @Transaction
    @Query("SELECT * FROM workouts " +
            "WHERE programId = :programId")
    suspend fun getAllForProgram(programId: Long): List<Workout>
}