package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.entities.HistoricalWorkoutNameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoricalWorkoutNamesDao: BaseDao<HistoricalWorkoutNameEntity> {
    @Query("SELECT * FROM historicalWorkoutNames WHERE synced = 0")
    suspend fun getAllUnsynced(): List<HistoricalWorkoutNameEntity>

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames WHERE deleted = 0")
    suspend fun getAll(): List<HistoricalWorkoutNameEntity>

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<HistoricalWorkoutNameEntity>

    @Query("SELECT * FROM historicalWorkoutNames WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): HistoricalWorkoutNameEntity?

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames WHERE deleted = 0")
    fun getAllFlow(): Flow<List<HistoricalWorkoutNameEntity>>

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames WHERE historical_workout_name_id = :id AND deleted = 0")
    suspend fun get(id: Long): HistoricalWorkoutNameEntity?

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames WHERE historical_workout_name_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<HistoricalWorkoutNameEntity>

    @Query("DELETE FROM historicalWorkoutNames")
    suspend fun deleteAll()

    @Query("SELECT * FROM historicalWorkoutNames " +
            "WHERE programId = :programId AND workoutId = :workoutId AND deleted = 0")
    suspend fun getByProgramAndWorkoutId(programId: Long, workoutId: Long): HistoricalWorkoutNameEntity?

    @Query("UPDATE historicalWorkoutNames SET deleted = 1, synced = 0 WHERE historical_workout_name_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE historicalWorkoutNames SET deleted = 1, synced = 0 WHERE historical_workout_name_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int
}