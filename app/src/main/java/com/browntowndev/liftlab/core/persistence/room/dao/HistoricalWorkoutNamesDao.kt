package com.browntowndev.liftlab.core.persistence.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.entities.room.HistoricalWorkoutNameEntity

@Dao
interface HistoricalWorkoutNamesDao: BaseDao<HistoricalWorkoutNameEntity> {
    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames")
    suspend fun getAll(): List<HistoricalWorkoutNameEntity>

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames WHERE historical_workout_name_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<HistoricalWorkoutNameEntity>

    @Query("DELETE FROM historicalWorkoutNames")
    suspend fun deleteAll()

    @Query("SELECT * FROM historicalWorkoutNames " +
            "WHERE programId = :programId AND workoutId = :workoutId")
    suspend fun getByProgramAndWorkoutId(programId: Long, workoutId: Long): HistoricalWorkoutNameEntity?
}