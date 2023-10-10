package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import java.util.Date

@Dao
interface LoggingDao {
    @Insert
    suspend fun insert(workoutLogEntry: WorkoutLogEntry): Long

    @Query("SELECT * FROM setLogEntries")
    suspend fun getAll(): List<SetLogEntry>

    @Query("INSERT INTO setLogEntries " +
            "(workoutLogEntryId, liftId, setType, " +
            "setPosition, myoRepSetPosition, weight, " +
            "reps, rpe, mesoCycle, microCycle) " +
            "SELECT :workoutLogEntryId, liftId, setType, " +
            "setPosition, myoRepSetPosition, weight, " +
            "reps, rpe, mesoCycle, microCycle " +
            "FROM previousSetResults")
    suspend fun insertFromPreviousSetResults(workoutLogEntryId: Long)

    @Query("SELECT log.historicalWorkoutNameId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "log.durationInMillis, setResult.setType, setResult.setPosition, setResult.myoRepSetPosition, " +
            "setResult.weight, setResult.reps, setResult.rpe, setResult.mesoCycle, setResult.microCycle " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE setResult.liftId = :liftId")
    suspend fun getLogsByLiftId(liftId: Long): List<FlattenedWorkoutLogEntryDto>

    @Query("SELECT log.historicalWorkoutNameId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "log.durationInMillis, setResult.setType, setResult.setPosition, setResult.myoRepSetPosition, " +
            "setResult.weight, setResult.reps, setResult.rpe, setResult.mesoCycle, setResult.microCycle " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE log.date BETWEEN :start AND :end")
    suspend fun getForDateRange(start: Date, end: Date): List<FlattenedWorkoutLogEntryDto>
}