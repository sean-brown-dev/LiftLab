package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Workout
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutsDao: BaseDao<Workout> {
    @Query("SELECT * FROM workouts WHERE workout_id = :id")
    suspend fun get(id: Long): Workout?

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<Workout>

    @Transaction
    @Query("SELECT * FROM workouts")
    suspend fun getAll(): List<Workout>

    @Transaction
    @Query("SELECT * FROM workouts")
    fun getAllFlow(): Flow<List<Workout>>

    @Query("DELETE FROM workouts")
    suspend fun deleteAll()

    @Query("DELETE FROM workouts WHERE workout_id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id = :id")
    suspend fun getWithRelationships(id: Long) : WorkoutWithRelationships?

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id = :id")
    fun getWithRelationshipsFlow(id: Long) : Flow<WorkoutWithRelationships?>

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