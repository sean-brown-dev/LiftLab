package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.dtos.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLogEntryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface WorkoutLogEntryDao: BaseDao<WorkoutLogEntryEntity> {
    @Query("SELECT * FROM workoutLogEntries WHERE synced = 0")
    suspend fun getAllUnsynced(): List<WorkoutLogEntryEntity>

    @Transaction
    @Query("SELECT * FROM workoutLogEntries WHERE deleted = 0")
    fun getAll(): List<WorkoutLogEntryEntity>

    @Query("SELECT * FROM workoutLogEntries WHERE workout_log_entry_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<WorkoutLogEntryEntity>

    @Query("DELETE FROM workoutLogEntries")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, setResult.set_log_entry_id as 'setLogEntryId', histWorkoutName.programId, histWorkoutName.workoutId, histWorkoutName.programName, histWorkoutName.workoutName, " +
            "log.programDeloadWeek, log.programWorkoutCount, log.microcyclePosition, log.date, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.oneRepMax, log.mesoCycle, log.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition, setResult.isDeload " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE log.deleted = 0 AND " +
            "setResult.deleted = 0")
    fun getAllFlattenedFlow(): Flow<List<FlattenedWorkoutLogEntryDto>>

    @Transaction
    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, setResult.set_log_entry_id as 'setLogEntryId', histWorkoutName.programId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.oneRepMax, log.mesoCycle, log.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition, setResult.isDeload " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE setResult.liftId = :liftId AND " +
            "log.deleted = 0 AND " +
            "setResult.deleted = 0")
    fun getLogsByLiftIdFlow(liftId: Long): Flow<List<FlattenedWorkoutLogEntryDto>>

    @Transaction
    @Query("""
    WITH latest AS (
      SELECT
          s.liftId,
          s.setPosition,
          MAX(w.date) AS maxDate
      FROM setLogEntries s
      JOIN workoutLogEntries w
        ON w.workout_log_entry_id = s.workoutLogEntryId
      WHERE s.liftId IN (:liftIds)
        AND w.deleted = 0
        AND s.deleted = 0
        AND (s.isDeload = 0 OR :includeDeloads)
      GROUP BY s.liftId, s.setPosition
    )
    SELECT 
        log.workout_log_entry_id as 'id',
        log.historicalWorkoutNameId,
        setResult.set_log_entry_id as 'setLogEntryId',
        histWorkoutName.programId,
        histWorkoutName.programName,
        histWorkoutName.workoutName,
        log.date,
        histWorkoutName.workoutId,
        log.programDeloadWeek,
        log.programWorkoutCount,
        log.durationInMillis,
        setResult.liftId,
        setResult.liftName,
        setResult.setType,
        setResult.liftPosition,
        setResult.setPosition,
        setResult.myoRepSetPosition,
        setResult.weight,
        setResult.reps,
        setResult.rpe,
        setResult.oneRepMax,
        log.mesoCycle,
        log.microCycle,
        setResult.progressionScheme,
        setResult.liftMovementPattern,
        setResult.repRangeBottom,
        setResult.repRangeTop,
        setResult.weightRecommendation,
        setResult.setMatching,
        setResult.rpeTarget,
        setResult.maxSets,
        setResult.repFloor,
        setResult.dropPercentage,
        log.microcyclePosition,
        setResult.isDeload
    FROM workoutLogEntries log
    JOIN historicalWorkoutNames histWorkoutName
      ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId
    JOIN setLogEntries setResult
      ON setResult.workoutLogEntryId = log.workout_log_entry_id
    JOIN latest L
      ON L.liftId = setResult.liftId
     AND L.setPosition = setResult.setPosition
     AND L.maxDate = log.date
    WHERE log.deleted = 0
      AND setResult.deleted = 0
""")
    suspend fun getMostRecentLogsForLiftIds(
        liftIds: List<Long>,
        includeDeloads: Boolean
    ): List<FlattenedWorkoutLogEntryDto>

    @Transaction
    @Query("""
    SELECT 
        log.workout_log_entry_id AS 'id',
        log.historicalWorkoutNameId,
        setResult.set_log_entry_id AS 'setLogEntryId',
        histWorkoutName.programId,
        histWorkoutName.programName,
        histWorkoutName.workoutName,
        log.date,
        histWorkoutName.workoutId,
        log.programDeloadWeek,
        log.programWorkoutCount,
        log.durationInMillis,
        setResult.liftId,
        setResult.liftName,
        setResult.setType,
        setResult.liftPosition,
        setResult.setPosition,
        setResult.myoRepSetPosition,
        setResult.weight,
        setResult.reps,
        setResult.rpe,
        setResult.oneRepMax,
        log.mesoCycle,
        log.microCycle,
        setResult.progressionScheme,
        setResult.liftMovementPattern,
        setResult.repRangeBottom,
        setResult.repRangeTop,
        setResult.weightRecommendation,
        setResult.setMatching,
        setResult.rpeTarget,
        setResult.maxSets,
        setResult.repFloor,
        setResult.dropPercentage,
        log.microcyclePosition,
        setResult.isDeload
    FROM workoutLogEntries AS log
    JOIN historicalWorkoutNames AS histWorkoutName
      ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId
    JOIN setLogEntries AS setResult
      ON setResult.workoutLogEntryId = log.workout_log_entry_id
    -- latest date prior to :date per (liftId, setPosition)
    JOIN (
      SELECT
          s.liftId,
          s.setPosition,
          MAX(w.date) AS maxDate
      FROM setLogEntries AS s
      JOIN workoutLogEntries AS w
        ON w.workout_log_entry_id = s.workoutLogEntryId
      WHERE s.liftId IN (:liftIds)
        AND w.date < :date
        AND w.deleted = 0
        AND s.deleted = 0
      GROUP BY s.liftId, s.setPosition
    ) AS L
      ON L.liftId      = setResult.liftId
     AND L.setPosition = setResult.setPosition
     AND L.maxDate     = log.date
    WHERE log.deleted = 0
      AND setResult.deleted = 0
""")
    suspend fun getMostRecentLogsForLiftIdsPriorToDate(
        liftIds: List<Long>,
        date: Date
    ): List<FlattenedWorkoutLogEntryDto>

    @Transaction
    @Query("SELECT log.workout_log_entry_id as 'id', log.historicalWorkoutNameId, setResult.set_log_entry_id as 'setLogEntryId', histWorkoutName.programId, histWorkoutName.programName, histWorkoutName.workoutName, log.date, " +
            "histWorkoutName.workoutId, log.programDeloadWeek, log.programWorkoutCount, log.durationInMillis, setResult.liftId, setResult.liftName, setResult.setType, " +
            "setResult.liftPosition, setResult.setPosition, setResult.myoRepSetPosition, setResult.weight, setResult.reps, setResult.rpe, setResult.oneRepMax, log.mesoCycle, log.microCycle, " +
            "setResult.progressionScheme, setResult.liftMovementPattern, setResult.repRangeBottom, setResult.repRangeTop, setResult.weightRecommendation, setResult.setMatching, " +
            "setResult.rpeTarget, setResult.maxSets, setResult.repFloor, setResult.dropPercentage, log.microcyclePosition, setResult.isDeload " +
            "FROM workoutLogEntries log " +
            "INNER JOIN historicalWorkoutNames histWorkoutName ON histWorkoutName.historical_workout_name_id = log.historicalWorkoutNameId " +
            "INNER JOIN setLogEntries setResult ON setResult.workoutLogEntryId = log.workout_log_entry_id " +
            "WHERE log.workout_log_entry_id = :workoutLogEntryId AND " +
            "log.deleted = 0 AND " +
            "setResult.deleted = 0")
    fun getFlattenedFlow(workoutLogEntryId: Long): Flow<List<FlattenedWorkoutLogEntryDto>>

    @Query("SELECT * FROM workoutLogEntries WHERE workout_log_entry_id = :workoutLogEntryId AND deleted = 0")
    suspend fun get(workoutLogEntryId: Long): WorkoutLogEntryEntity?

    @Query("UPDATE workoutLogEntries SET deleted = 1, synced = 0 WHERE workout_log_entry_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE workoutLogEntries SET deleted = 1, synced = 0 WHERE workout_log_entry_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM workoutLogEntries WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): WorkoutLogEntryEntity?

    @Query("SELECT * FROM workoutLogEntries WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<WorkoutLogEntryEntity>

    @Query("""
        SELECT programId 
        FROM workoutLogEntries wle
        INNER JOIN historicalWorkoutNames hwn ON hwn.historical_workout_name_id = wle.historicalWorkoutNameId
        WHERE workout_log_entry_id = :workoutLogEntryId
    """)
    fun getProgramId(workoutLogEntryId: Long): Long?
}