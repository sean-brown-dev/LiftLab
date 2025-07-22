package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.WorkoutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutsDao: BaseDao<WorkoutEntity> {
    @Query("SELECT * FROM workouts WHERE synced = 0")
    suspend fun getAllUnsynced(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE workout_id = :id AND deleted = 0")
    suspend fun get(id: Long): WorkoutWithRelationships?

    @Query("SELECT * FROM workouts WHERE workout_id = :id AND deleted = 0")
    suspend fun getWithoutRelationships(id: Long): WorkoutEntity?

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<WorkoutWithRelationships>

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id IN (:ids) AND deleted = 0")
    suspend fun getManyWithoutRelationships(ids: List<Long>): List<WorkoutEntity>

    @Transaction
    @Query("SELECT * FROM workouts WHERE deleted = 0")
    suspend fun getAll(): List<WorkoutWithRelationships>

    @Transaction
    @Query("SELECT * FROM workouts WHERE deleted = 0")
    fun getAllFlow(): Flow<List<WorkoutWithRelationships>>

    @Query("DELETE FROM workouts")
    suspend fun deleteAll()

    @Query("DELETE FROM workouts WHERE workout_id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM workouts WHERE workout_id = :id AND deleted = 0")
    fun getByIdFlow(id: Long) : Flow<WorkoutWithRelationships?>

    @Query("SELECT MAX(position) FROM workouts WHERE programId = :programId AND deleted = 0")
    suspend fun getFinalPosition(programId: Long): Int

    @Transaction
    @Query("SELECT * FROM workouts " +
            "WHERE position = :microcyclePosition AND " +
            "programId = :programId AND deleted = 0")
    fun getByMicrocyclePosition(programId: Long, microcyclePosition: Int): Flow<WorkoutWithRelationships?>

    @Transaction
    @Query("SELECT * FROM workouts " +
            "WHERE programId = :programId AND deleted = 0")
    suspend fun getAllForProgram(programId: Long): List<WorkoutWithRelationships>

    @Transaction
    @Query("SELECT * FROM workouts " +
            "WHERE programId = :programId AND deleted = 0")
    suspend fun getAllForProgramWithoutRelationships(programId: Long): List<WorkoutEntity>

    @Query("UPDATE workouts SET deleted = 1, synced = 0 WHERE workout_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE workouts SET deleted = 1, synced = 0 WHERE workout_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM workouts WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<WorkoutEntity>
}