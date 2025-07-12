package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName

@Dao
interface HistoricalWorkoutNamesDao: BaseDao<HistoricalWorkoutName> {
    @Query("SELECT * FROM historicalWorkoutNames")
    suspend fun getAll(): List<HistoricalWorkoutName>

    @Query("DELETE FROM historicalWorkoutNames")
    suspend fun deleteAll()

    @Query("SELECT * FROM historicalWorkoutNames " +
            "WHERE programId = :programId AND workoutId = :workoutId")
    suspend fun getByProgramAndWorkoutId(programId: Long, workoutId: Long): HistoricalWorkoutName?
}