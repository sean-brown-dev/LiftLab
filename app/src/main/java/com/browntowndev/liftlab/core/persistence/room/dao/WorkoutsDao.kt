package com.browntowndev.liftlab.core.persistence.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.room.dtos.WorkoutWithRelationships
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutsDao: BaseDao<WorkoutEntity> {
    @Query("SELECT * FROM workouts WHERE workout_id = :id")
    suspend fun get(id: Long): WorkoutEntity?

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<WorkoutEntity>

    @Transaction
    @Query("SELECT * FROM workouts")
    suspend fun getAll(): List<WorkoutEntity>

    @Transaction
    @Query("SELECT * FROM workouts")
    fun getAllFlow(): Flow<List<WorkoutEntity>>

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
    suspend fun getAllForProgram(programId: Long): List<WorkoutEntity>
}