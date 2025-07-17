package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName

@Dao
interface HistoricalWorkoutNamesDao: BaseDao<HistoricalWorkoutName> {
    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames")
    suspend fun getAll(): List<HistoricalWorkoutName>

    @Transaction
    @Query("SELECT * FROM historicalWorkoutNames WHERE historical_workout_name_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<HistoricalWorkoutName>

    @Query("DELETE FROM historicalWorkoutNames")
    suspend fun deleteAll()

    @Query("SELECT * FROM historicalWorkoutNames " +
            "WHERE programId = :programId AND workoutId = :workoutId")
    suspend fun getByProgramAndWorkoutId(programId: Long, workoutId: Long): HistoricalWorkoutName?
}