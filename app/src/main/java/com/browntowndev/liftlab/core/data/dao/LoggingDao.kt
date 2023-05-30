package com.browntowndev.liftlab.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.dtos.LiftLogDto
import com.browntowndev.liftlab.core.data.dtos.WorkoutLogDto
import com.browntowndev.liftlab.core.data.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.data.entities.SetLogEntry
import com.browntowndev.liftlab.core.data.entities.WorkoutLogEntry

@Dao
interface LoggingDao {
    @Insert
    suspend fun insert(setLogEntry: SetLogEntry)

    @Insert
    suspend fun insert(workoutLogEntry: WorkoutLogEntry)

    @Insert
    suspend fun insert(historicalWorkoutName: HistoricalWorkoutName)

    @Transaction
    @Query("SELECT * FROM setLogEntries WHERE liftId = :liftId")
    suspend fun getForLift(liftId: Long): List<LiftLogDto>

    @Transaction
    @Query("SELECT * FROM workoutLogEntries WHERE historicalWorkoutNameId = :historicalWorkoutNameId")
    suspend fun getForWorkout(historicalWorkoutNameId: Long): List<WorkoutLogDto>
}