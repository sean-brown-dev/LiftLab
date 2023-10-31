package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LoggingDao {
    @Insert
    suspend fun insert(workoutLogEntry: WorkoutLogEntry): Long

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "log.durationInMillis, lift.lift_id as 'liftId', lift.name as 'liftName', setResult.setType, setResult.setPosition, setResult.myoRepSetPosition, " +
            "setResult.weight, setResult.reps, setResult.rpe, setResult.mesoCycle, setResult.microCycle " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "INNER JOIN lifts lift ON setResult.liftId = lift.lift_id")
    fun getAll(): Flow<List<FlattenedWorkoutLogEntryDto>>

    @Query("INSERT INTO setLogEntries " +
            "(workoutLogEntryId, liftId, setType, " +
            "setPosition, myoRepSetPosition, weight, " +
            "reps, rpe, mesoCycle, microCycle) " +
            "SELECT :workoutLogEntryId, liftId, setType, " +
            "setPosition, myoRepSetPosition, weight, " +
            "reps, rpe, mesoCycle, microCycle " +
            "FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "previously_completed_set_id NOT IN (:excludeFromCopy)")
    suspend fun insertFromPreviousSetResults(workoutLogEntryId: Long, workoutId: Long, excludeFromCopy: List<Long>)

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "log.durationInMillis, lift.lift_id as 'liftId', lift.name as 'liftName', setResult.setType, setResult.setPosition, setResult.myoRepSetPosition, " +
            "setResult.weight, setResult.reps, setResult.rpe, setResult.mesoCycle, setResult.microCycle " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "INNER JOIN lifts lift ON setResult.liftId = lift.lift_id " +
            "WHERE setResult.liftId = :liftId")
    suspend fun getLogsByLiftId(liftId: Long): List<FlattenedWorkoutLogEntryDto>

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "log.durationInMillis, lift.lift_id as 'liftId', lift.name as 'liftName', setResult.setType, setResult.setPosition, setResult.myoRepSetPosition, " +
            "setResult.weight, setResult.reps, setResult.rpe, setResult.mesoCycle, setResult.microCycle " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "INNER JOIN lifts lift ON setResult.liftId = lift.lift_id " +
            "WHERE histWorkoutName.workoutId = :workoutId AND " +
            "log.mesocycle = :mesoCycle AND " +
            "log.microcycle = :microCycle")
    suspend fun getForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<FlattenedWorkoutLogEntryDto>
}