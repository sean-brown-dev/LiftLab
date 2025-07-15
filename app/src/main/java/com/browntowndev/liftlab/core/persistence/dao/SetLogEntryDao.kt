package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.dtos.queryable.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry

@Dao
interface SetLogEntryDao: BaseDao<SetLogEntry> {
    @Query("SELECT * FROM setLogEntries WHERE set_log_entry_id = :id")
    suspend fun get(id: Long): SetLogEntry?

    @Transaction
    @Query("SELECT * FROM setLogEntries WHERE set_log_entry_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<SetLogEntry>

    @Transaction
    @Query("SELECT * FROM setLogEntries")
    suspend fun getAll(): List<SetLogEntry>

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
        weightRecommendation,
        reps,
        rpe,
        oneRepMax,
        mesoCycle,
        microCycle,
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
        sr.weightRecommendation,
        sr.reps,
        sr.rpe,
        sr.oneRepMax,
        sr.mesoCycle,
        sr.microCycle,
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
    FROM previousSetResults sr
    INNER JOIN workoutLifts wl ON wl.liftId = sr.liftId AND wl.position = sr.liftPosition
    LEFT JOIN sets s ON s.workoutLiftId = wl.workout_lift_id AND s.position = sr.setPosition
    INNER JOIN lifts l ON l.lift_id = wl.liftId
    WHERE
        sr.workoutId = :workoutId AND
        wl.workoutId = :workoutId AND
        sr.mesoCycle = :mesocycle AND
        sr.microCycle = :microcycle AND
        sr.previously_completed_set_id NOT IN (:excludeFromCopy)
    """)
    suspend fun insertFromPreviousSetResults(
        workoutLogEntryId: Long,
        workoutId: Long,
        mesocycle: Int,
        microcycle: Int,
        excludeFromCopy: List<Long>
    )

    @Transaction
    @Query("""
    SELECT *
    FROM setLogEntries
    WHERE workoutLogEntryId = :workoutLogEntryId
      AND mesoCycle = :mesocycle
      AND microCycle = :microcycle
""")
    suspend fun getForWorkoutLogEntryMesoAndMicro(
        workoutLogEntryId: Long,
        mesocycle: Int,
        microcycle: Int
    ): List<SetLogEntry>

    @Transaction
    @Query("""
    SELECT
        liftId,
        MAX(oneRepMax) AS personalRecord
    FROM setLogEntries
    WHERE liftId IN (:liftIds)
    GROUP BY liftId
    """)
    suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecordDto>

    @Transaction
    @Query("SELECT * FROM setLogEntries WHERE workoutLogEntryId = :workoutLogEntryId")
    suspend fun getForWorkoutLogEntry(workoutLogEntryId: Long): List<SetLogEntry>

    @Query("DELETE FROM setLogEntries WHERE set_log_entry_id IN (:ids)")
    suspend fun deleteManySetLogEntries(ids: List<Long>)
}