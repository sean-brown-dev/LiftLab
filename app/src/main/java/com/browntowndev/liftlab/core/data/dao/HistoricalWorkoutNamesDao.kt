package com.browntowndev.liftlab.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.data.entities.HistoricalWorkoutName

@Dao
interface HistoricalWorkoutNamesDao {
    @Insert
    suspend fun insert(historyEntry: HistoricalWorkoutName): Long

    @Query("SELECT historical_workout_name_id " +
            "FROM historicalWorkoutNames " +
            "WHERE historical_workout_name_id = :id OR (programName = :programName AND workoutName = :workoutName)")
    suspend fun get(id: Long, programName: String, workoutName: String)
}