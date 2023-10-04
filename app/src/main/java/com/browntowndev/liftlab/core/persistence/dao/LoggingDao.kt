package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntries
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry

@Dao
interface LoggingDao {
    @Insert
    suspend fun insert(workoutLogEntry: WorkoutLogEntry): Long

    @Query("INSERT INTO setLogEntries " +
            "(workoutLogEntryId, liftId, customSetType, " +
            "setPosition, myoRepSetPosition, weight, " +
            "reps, rpe, mesoCycle, microCycle) " +
            "SELECT :workoutLogEntryId, liftId, setType, " +
            "setPosition, myoRepSetPosition, weight, " +
            "reps, rpe, mesoCycle, microCycle " +
            "FROM previousSetResults")
    suspend fun insertFromPreviousSetResults(workoutLogEntryId: Long)

    @Query("SELECT log.historicalWorkoutNameId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, log.durationInMillis, " +
            "setResult.setType, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, " +
            "setResult.mesoCycle, setResult.microCycle, setResult.missedLpGoals " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON log.historicalWorkoutNameId = histWorkoutName.historical_workout_name_id " +
            "INNER JOIN previousSetResults setResult ON histWorkoutName.workoutId = setResult.workoutId " +
            "WHERE setResult.liftId = :liftId")
    suspend fun getLogsByLiftId(liftId: Long): List<FlattenedWorkoutLogEntries>
}