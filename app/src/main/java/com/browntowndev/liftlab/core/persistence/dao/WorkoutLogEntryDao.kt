package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WorkoutLogEntryDao: BaseDao<WorkoutLogEntry> {
    @Query("SELECT * FROM workoutLogEntries")
    fun getAll(): List<WorkoutLogEntry>

    @Query("DELETE FROM workoutLogEntries")
    suspend fun deleteAll()

    @Query("DELETE FROM workoutLogEntries WHERE workout_log_entry_id = :workoutLogEntryId")
    suspend fun deleteWorkoutLogEntry(workoutLogEntryId: Long)

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, setResult.set_log_entry_id as 'setLogEntryId', histWorkoutName.programId, histWorkoutName.workoutId, histWorkoutName.programName, histWorkoutName.workoutName, " +
            "log.programDeloadWeek, log.programWorkoutCount, log.microcyclePosition, log.date, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.oneRepMax, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition, setResult.isDeload " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id")
    fun getAllFlattened(): Flow<List<FlattenedWorkoutLogEntryDto>>

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, setResult.set_log_entry_id as 'setLogEntryId', histWorkoutName.programId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.oneRepMax, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition, setResult.isDeload " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE setResult.liftId = :liftId")
    suspend fun getLogsByLiftId(liftId: Long): List<FlattenedWorkoutLogEntryDto>

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, setResult.set_log_entry_id as 'setLogEntryId', histWorkoutName.programId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.oneRepMax, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition, setResult.isDeload " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE setResult.liftId IN (:liftIds) AND " +
            "log.date = " +
            "(SELECT MAX(log2.date) " +
            "FROM workoutLogEntries log2 " +
            "INNER JOIN setLogEntries setResult2 ON setResult2.workoutLogEntryId = log2.workout_log_entry_id " +
            "WHERE setResult2.liftId = setResult.liftId AND " +
            "setResult2.setPosition = setResult.setPosition AND " +
            "setResult2.isDeload = 0)")
    suspend fun getMostRecentLogsForLiftIdsExcludingDeloads(liftIds: List<Long>): List<FlattenedWorkoutLogEntryDto>

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, setResult.set_log_entry_id as 'setLogEntryId', histWorkoutName.programId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.oneRepMax, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition, setResult.isDeload " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE setResult.liftId IN (:liftIds) AND " +
            "log.date = " +
            "(SELECT MAX(log2.date) " +
            "FROM workoutLogEntries log2 " +
            "INNER JOIN setLogEntries setResult2 ON setResult2.workoutLogEntryId = log2.workout_log_entry_id " +
            "WHERE setResult2.liftId = setResult.liftId AND " +
            "setResult2.setPosition = setResult.setPosition)")
    suspend fun getMostRecentLogsForLiftIds(liftIds: List<Long>): List<FlattenedWorkoutLogEntryDto>

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, setResult.set_log_entry_id as 'setLogEntryId', histWorkoutName.programId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.oneRepMax, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition, setResult.isDeload " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE setResult.liftId IN (:liftIds) AND " +
            "log.date = " +
            "(SELECT MAX(log2.date) " +
            "FROM workoutLogEntries log2 " +
            "INNER JOIN setLogEntries setResult2 ON setResult2.workoutLogEntryId = log2.workout_log_entry_id " +
            "WHERE setResult2.liftId = setResult.liftId AND " +
            "setResult2.setPosition = setResult.setPosition AND " +
            "log2.date < :date)")
    suspend fun getMostRecentLogsForLiftIdsPriorToDate(liftIds: List<Long>, date: Date): List<FlattenedWorkoutLogEntryDto>

    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, setResult.set_log_entry_id as 'setLogEntryId', histWorkoutName.programId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.oneRepMax, setResult.mesoCycle, setResult.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition, setResult.isDeload " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "INNER JOIN lifts lift ON setResult.liftId = lift.lift_id " +
            "WHERE log.workout_log_entry_id = :workoutLogEntryId")
    suspend fun get(workoutLogEntryId: Long): List<FlattenedWorkoutLogEntryDto>
}