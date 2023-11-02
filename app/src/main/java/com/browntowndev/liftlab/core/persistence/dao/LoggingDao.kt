package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface LoggingDao {
    @Insert
    suspend fun insert(workoutLogEntry: WorkoutLogEntry): Long

    @Query("INSERT INTO setLogEntries " +
            "(workoutLogEntryId, workoutLiftDeloadWeek, liftId, setType, liftPosition, progressionScheme, " +
            "setPosition, myoRepSetPosition, weight, liftName, liftMovementPattern, weightRecommendation, " +
            "reps, rpe, mesoCycle, microCycle, repRangeBottom, repRangeTop, rpeTarget, setMatching, maxSets, " +
            "repFloor, dropPercentage) " +
            "SELECT :workoutLogEntryId, wl.deloadWeek, sr.liftId, setType, liftPosition, wl.progressionScheme, " +
            "setPosition, myoRepSetPosition, weight, l.name, l.movementPattern, sr.weightRecommendation, " +
            "reps, rpe, mesoCycle, microCycle, wl.repRangeBottom, wl.repRangeTop, wl.rpeTarget, s.setMatching, s.maxSets, " +
            "s.repFloor, s.dropPercentage " +
            "FROM previousSetResults sr " +
            "INNER JOIN workoutLifts wl ON (wl.liftId = sr.liftId AND wl.position = sr.liftPosition) " +
            "LEFT JOIN sets s ON s.workoutLiftId = wl.workout_lift_id " +
            "INNER JOIN lifts l ON l.lift_id = wl.liftId " +
            "WHERE sr.workoutId = :workoutId AND " +
            "wl.workoutId = :workoutId AND " +
            "sr.previously_completed_set_id NOT IN (:excludeFromCopy)")
    suspend fun insertFromPreviousSetResults(workoutLogEntryId: Long, workoutId: Long, excludeFromCopy: List<Long>)

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, histWorkoutName.workoutId, histWorkoutName.programName, histWorkoutName.workoutName, " +
            "log.programDeloadWeek, log.programWorkoutCount, log.microcyclePosition, log.date, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id")
    fun getAll(): Flow<List<FlattenedWorkoutLogEntryDto>>

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE setResult.liftId = :liftId")
    suspend fun getLogsByLiftId(liftId: Long): List<FlattenedWorkoutLogEntryDto>

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "INNER JOIN lifts lift ON setResult.liftId = lift.lift_id " +
            "WHERE log.workout_log_entry_id = :workoutLogEntryId")
    suspend fun get(workoutLogEntryId: Long): List<FlattenedWorkoutLogEntryDto>

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "INNER JOIN lifts lift ON setResult.liftId = lift.lift_id " +
            "WHERE histWorkoutName.historical_workout_name_id = :historicalWorkoutNameId AND " +
            "log.date < :date " +
            "ORDER BY log.date DESC ")
    suspend fun getFirstPriorToDate(historicalWorkoutNameId: Long, date: Date): List<FlattenedWorkoutLogEntryDto>

    @Query("DELETE FROM setLogEntries " +
            "WHERE set_log_entry_id IN (" +
            "SELECT set_log_entry_id " +
            "FROM setLogEntries setLog " +
            "INNER JOIN workoutLogEntries workoutLog ON workoutLog.workout_log_entry_id = setLog.workoutLogEntryId " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = workoutLog.historicalWorkoutNameId " +
            "WHERE histWorkoutName.workoutId = :workoutId AND " +
            "setLog.liftPosition = :liftPosition AND " +
            "setLog.setPosition = :setPosition AND " +
            "setLog.myoRepSetPosition = :myoRepSetPosition )")
    suspend fun delete(workoutId: Long, liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?)

    @Upsert
    suspend fun upsert(setLogEntry: SetLogEntry): Long

    @Upsert
    suspend fun upsertMany(setLogEntries: List<SetLogEntry>): List<Long>
}