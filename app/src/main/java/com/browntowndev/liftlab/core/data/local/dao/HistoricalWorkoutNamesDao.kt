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
    @Query("SELECT * FROM historicalWorkoutNames")
    suspend fun getAll(): List<HistoricalWorkoutNameEntity>

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames")
    fun getAllFlow(): Flow<List<HistoricalWorkoutNameEntity>>

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames WHERE historical_workout_name_id = :id")
    suspend fun get(id: Long): HistoricalWorkoutNameEntity?

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames WHERE historical_workout_name_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<HistoricalWorkoutNameEntity>

    @Query("DELETE FROM historicalWorkoutNames")
    suspend fun deleteAll()

    @Query("SELECT * FROM historicalWorkoutNames " +
            "WHERE programId = :programId AND workoutId = :workoutId")
    suspend fun getByProgramAndWorkoutId(programId: Long, workoutId: Long): HistoricalWorkoutNameEntity?

    @Query("UPDATE historicalWorkoutNames SET deleted = 1, synced = 0 WHERE historical_workout_name_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE historicalWorkoutNames SET deleted = 1, synced = 0 WHERE historical_workout_name_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int
}