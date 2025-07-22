package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.browntowndev.liftlab.core.data.local.entities.WorkoutInProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutInProgressDao: BaseDao<WorkoutInProgressEntity> {
    @Query("SELECT * FROM workoutsInProgress WHERE synced = 0")
    suspend fun getAllUnsynced(): List<WorkoutInProgressEntity>

    @Query("DELETE FROM workoutsInProgress")
    suspend fun deleteAll()

    @Query("SELECT * FROM workoutsInProgress")
    suspend fun get(): WorkoutInProgressEntity?

    @Query("SELECT * FROM workoutsInProgress")
    suspend fun getAll(): List<WorkoutInProgressEntity>

    @Query("SELECT * FROM workoutsInProgress WHERE workout_in_progress_id = :id")
    suspend fun get(id: Long): WorkoutInProgressEntity?

    @Query("SELECT * FROM workoutsInProgress")
    fun getAllFlow(): Flow<List<WorkoutInProgressEntity>>

    @Query("SELECT * FROM workoutsInProgress WHERE workout_in_progress_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<WorkoutInProgressEntity>

    @Query("DELETE FROM workoutsInProgress")
    suspend fun delete()

    @Query("UPDATE workoutsInProgress SET deleted = 1, synced = 0 WHERE workout_in_progress_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE workoutsInProgress SET deleted = 1, synced = 0 WHERE workout_in_progress_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM workoutsInProgress WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): WorkoutInProgressEntity?

    @Query("SELECT * FROM workoutsInProgress WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<WorkoutInProgressEntity>
}