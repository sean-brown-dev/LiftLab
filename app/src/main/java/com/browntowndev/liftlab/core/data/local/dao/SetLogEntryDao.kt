package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.dtos.PersonalRecordDto
import com.browntowndev.liftlab.core.data.local.entities.SetLogEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetLogEntryDao: BaseDao<SetLogEntryEntity> {
    @Query("SELECT * FROM setLogEntries WHERE synced = 0")
    suspend fun getAllUnsynced(): List<SetLogEntryEntity>

    @Query("SELECT * FROM setLogEntries WHERE set_log_entry_id = :id AND deleted = 0")
    suspend fun get(id: Long): SetLogEntryEntity?

    @Transaction
    @Query("SELECT * FROM setLogEntries WHERE set_log_entry_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<SetLogEntryEntity>

    @Transaction
    @Query("SELECT * FROM setLogEntries WHERE deleted = 0")
    suspend fun getAll(): List<SetLogEntryEntity>

    @Transaction
    @Query("SELECT * FROM setLogEntries WHERE deleted = 0")
    fun getAllFlow(): Flow<List<SetLogEntryEntity>>

    @Transaction
    @Query("""
    SELECT sle.* FROM setLogEntries sle
    INNER JOIN workoutLogEntries wle ON wle.workout_log_entry_id = sle.workoutLogEntryId
    INNER JOIN historicalWorkoutNames hwn ON wle.historicalWorkoutNameId = hwn.historical_workout_name_id
    WHERE hwn.workoutId = :workoutId
      AND sle.deleted = 0
      AND (sle.isDeload = 0 OR :includeDeload)
      AND wle.mesoCycle = (
            SELECT MAX(wle2.mesoCycle)
            FROM setLogEntries sle2
            INNER JOIN workoutLogEntries wle2 ON wle2.workout_log_entry_id = sle2.workoutLogEntryId
            INNER JOIN historicalWorkoutNames hwn2 ON wle2.historicalWorkoutNameId = hwn2.historical_workout_name_id
            WHERE hwn2.workoutId = :workoutId
              AND sle2.deleted = 0
              AND (sle2.isDeload = 0 OR :includeDeload)
      )
      AND wle.microCycle = (
            SELECT MAX(wle3.microCycle)
            FROM setLogEntries sle3
            INNER JOIN workoutLogEntries wle3 ON wle3.workout_log_entry_id = sle3.workoutLogEntryId
            INNER JOIN historicalWorkoutNames hwn3 ON wle3.historicalWorkoutNameId = hwn3.historical_workout_name_id
            WHERE hwn3.workoutId = :workoutId
              AND sle3.deleted = 0
              AND (sle3.isDeload = 0 OR :includeDeload)
              AND wle3.mesoCycle = wle.mesoCycle
      )
""")
    fun getLatestForWorkout(
        workoutId: Long,
        includeDeload: Boolean,
    ): Flow<List<SetLogEntryEntity>>

    @Transaction
    @Query("""
        SELECT sle.* FROM setLogEntries sle
        INNER JOIN workoutLogEntries wle ON wle.workout_log_entry_id = sle.workoutLogEntryId
        INNER JOIN historicalWorkoutNames hwn ON wle.historicalWorkoutNameId = hwn.historical_workout_name_id
        WHERE workoutId = :workoutId AND 
        mesoCycle = :mesoCycle AND 
        microCycle = :microCycle AND 
        sle.deleted = 0
        """)
    fun getForSpecificWorkoutCompletionFlow(workoutId: Long, mesoCycle: Int, microCycle: Int): Flow<List<SetLogEntryEntity>>

    @Query("""
        SELECT sle.* FROM setLogEntries sle
        INNER JOIN workoutLogEntries wle ON wle.workout_log_entry_id = sle.workoutLogEntryId
        INNER JOIN historicalWorkoutNames hwn ON wle.historicalWorkoutNameId = hwn.historical_workout_name_id
        WHERE workoutId = :workoutId AND sle.deleted = 0
        """)
    suspend fun getForAllWorkoutCompletions(workoutId: Long): List<SetLogEntryEntity>

    @Transaction
    @Query("""
        SELECT * FROM setLogEntries 
        WHERE liftId = :liftId AND deleted = 0
        """)
    suspend fun getForLift(liftId: Long): List<SetLogEntryEntity>

    @Transaction
    @Query("""
        SELECT liftId, MAX(oneRepMax) as 'personalRecord' 
        FROM setLogEntries sle
        INNER JOIN workoutLogEntries wle ON wle.workout_log_entry_id = sle.workoutLogEntryId
        INNER JOIN historicalWorkoutNames hwn ON wle.historicalWorkoutNameId = hwn.historical_workout_name_id
        WHERE liftId IN (:liftIds) AND 
        (workoutId != :workoutId OR mesoCycle != :mesoCycle OR microCycle != :microCycle) 
        AND sle.deleted = 0 
        GROUP BY liftId
        """)
    suspend fun getPersonalRecordsForLiftsExcludingWorkout(
        workoutId: Long,
        mesoCycle: Int,
        microCycle: Int,
        liftIds: List<Long>,
    ): List<PersonalRecordDto>

    @Query("DELETE FROM setLogEntries")
    suspend fun deleteAll()

    @Query("""
    INSERT INTO setLogEntries (
        workoutLogEntryId,
        workoutLiftDeloadWeek,
        liftId,
        setType,
        liftPosition,
        progressionScheme,
        setPosition,
        myoRepSetPosition,
        weight,
        liftName,
        liftMovementPattern,
        reps,
        rpe,
        oneRepMax,
        repRangeBottom,
        repRangeTop,
        rpeTarget,
        setMatching,
        maxSets,
        repFloor,
        dropPercentage,
        isDeload
    )
    SELECT
        :workoutLogEntryId,
        wl.deloadWeek,
        sr.liftId,
        sr.setType,
        sr.liftPosition,
        wl.progressionScheme,
        sr.setPosition,
        sr.myoRepSetPosition,
        sr.weight,
        l.name,
        l.movementPattern,
        sr.reps,
        sr.rpe,
        sr.oneRepMax,
        wl.repRangeBottom,
        wl.repRangeTop,
        COALESCE(
            CASE
                WHEN sr.myoRepSetPosition IS NOT NULL THEN 10
                ELSE NULL
            END,
            s.rpeTarget,
            wl.rpeTarget
        ),
        s.setMatching,
        s.maxSets,
        s.repFloor,
        s.dropPercentage,
        sr.isDeload
    FROM liveWorkoutCompletedSets sr
    INNER JOIN workoutLifts wl ON wl.liftId = sr.liftId AND wl.position = sr.liftPosition
    LEFT JOIN sets s ON s.workoutLiftId = wl.workout_lift_id AND s.position = sr.setPosition
    INNER JOIN lifts l ON l.lift_id = wl.liftId
    WHERE (:excludeFromCopySize = 0 OR sr.live_workout_completed_set_id NOT IN (:excludeFromCopy))
    """)
    suspend fun insertFromLiveWorkoutCompletedSets(
        workoutLogEntryId: Long,
        excludeFromCopy: List<Long>,
        excludeFromCopySize: Int = excludeFromCopy.size,
    )

    @Transaction
    @Query("""
    SELECT sle.*
    FROM setLogEntries sle
    INNER JOIN workoutLogEntries wle ON wle.workout_log_entry_id = sle.workoutLogEntryId
    WHERE workoutLogEntryId = :workoutLogEntryId
      AND mesoCycle = :mesocycle
      AND microCycle = :microcycle 
      AND sle.deleted = 0
""")
    suspend fun getForWorkoutLogEntryMesoAndMicro(
        workoutLogEntryId: Long,
        mesocycle: Int,
        microcycle: Int
    ): List<SetLogEntryEntity>

    @Transaction
    @Query("""
    SELECT
        liftId,
        MAX(oneRepMax) AS personalRecord
    FROM setLogEntries
    WHERE liftId IN (:liftIds) AND deleted = 0
    GROUP BY liftId
    """)
    suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecordDto>

    @Transaction
    @Query("SELECT * FROM setLogEntries WHERE workoutLogEntryId = :workoutLogEntryId AND deleted = 0")
    suspend fun getForWorkoutLogEntry(workoutLogEntryId: Long): List<SetLogEntryEntity>

    @Query("DELETE FROM setLogEntries WHERE set_log_entry_id IN (:ids)")
    suspend fun deleteManySetLogEntries(ids: List<Long>)

    @Query("UPDATE setLogEntries SET deleted = 1, synced = 0 WHERE set_log_entry_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE setLogEntries SET deleted = 1, synced = 0 WHERE set_log_entry_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("UPDATE setLogEntries SET deleted = 1, synced = 0 WHERE workoutLogEntryId = :workoutLogEntryId")
    suspend fun softDeleteByWorkoutLogEntryId(workoutLogEntryId: Long): Int

    @Query("SELECT * FROM setLogEntries WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): SetLogEntryEntity?

    @Query("SELECT * FROM setLogEntries WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<SetLogEntryEntity>
}