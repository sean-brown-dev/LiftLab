package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.dtos.queryable.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult

@Dao
interface PreviousSetResultDao: BaseDao<PreviousSetResult> {
    @Query("SELECT * FROM previousSetResults WHERE previously_completed_set_id = :id")
    suspend fun get(id: Long): PreviousSetResult?

    @Transaction
    @Query("SELECT * FROM previousSetResults WHERE previously_completed_set_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<PreviousSetResult>

    @Transaction
    @Query("SELECT * FROM previousSetResults")
    suspend fun getAll(): List<PreviousSetResult>

    @Query("DELETE FROM previousSetResults")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "(mesoCycle != :mesoCycle OR " +
            "microCycle != :microCycle)")
    suspend fun getByWorkoutIdExcludingGivenMesoAndMicro(workoutId: Long, mesoCycle: Int, microCycle: Int): List<PreviousSetResult>

    @Transaction
    @Query("SELECT * FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "mesoCycle = :mesoCycle AND " +
            "microCycle = :microCycle")
    suspend fun getForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<PreviousSetResult>

    @Transaction
    @Query("SELECT * FROM previousSetResults " +
            "WHERE liftId = :liftId")
    suspend fun getForLift(liftId: Long): List<PreviousSetResult>

    @Transaction
    @Query("SELECT liftId, MAX(oneRepMax) as 'personalRecord' " +
            "FROM previousSetResults " +
            "WHERE liftId IN (:liftIds) AND " +
            "(workoutId != :workoutId OR mesoCycle != :mesoCycle OR microCycle != :microCycle) " +
            "GROUP BY liftId")
    suspend fun getPersonalRecordsForLiftsExcludingWorkout(
        workoutId: Long,
        mesoCycle: Int,
        microCycle: Int,
        liftIds: List<Long>,
    ): List<PersonalRecordDto>

    @Query("""
    SELECT * FROM previousSetResults
    WHERE previously_completed_set_id IN (
        SELECT sr.previously_completed_set_id
        FROM previousSetResults sr
        WHERE sr.workoutId = :workoutId
          AND (
              sr.previously_completed_set_id IN (:currentResultsToDelete)
              OR (
                  sr.mesoCycle != :currentMesocycle
                  OR sr.microCycle != :currentMicrocycle
              )
          )
          AND sr.previously_completed_set_id NOT IN (
              SELECT psr.previously_completed_set_id
              FROM previousSetResults dsr
              INNER JOIN previousSetResults psr ON
                  dsr.liftId = psr.liftId AND
                  dsr.liftPosition = psr.liftPosition AND
                  dsr.workoutId = psr.workoutId
              WHERE dsr.previously_completed_set_id IN (:currentResultsToDelete)
                AND psr.previously_completed_set_id NOT IN (:currentResultsToDelete)
          )
    )
""")
    suspend fun getAllForPreviousWorkout(
        workoutId: Long,
        currentMesocycle: Int,
        currentMicrocycle: Int,
        currentResultsToDelete: List<Long>,
    ): List<PreviousSetResult>

    @Query("SELECT * FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "mesoCycle = :mesoCycle AND " +
            "microCycle = :microCycle")
    suspend fun getAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<PreviousSetResult>
}