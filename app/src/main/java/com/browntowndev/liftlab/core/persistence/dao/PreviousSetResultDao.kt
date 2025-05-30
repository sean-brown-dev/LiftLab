package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.dtos.queryable.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult

@Dao
interface PreviousSetResultDao {
    @Query("SELECT * FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "(mesoCycle != :mesoCycle OR " +
            "microCycle != :microCycle)")
    suspend fun getByWorkoutIdExcludingGivenMesoAndMicro(workoutId: Long, mesoCycle: Int, microCycle: Int): List<PreviousSetResult>

    @Query("SELECT * FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "mesoCycle = :mesoCycle AND " +
            "microCycle = :microCycle")
    suspend fun getForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<PreviousSetResult>

    @Query("SELECT * FROM previousSetResults " +
            "WHERE liftId = :liftId")
    suspend fun getForLift(liftId: Long): List<PreviousSetResult>

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

    @Upsert
    suspend fun upsert(result: PreviousSetResult): Long

    @Upsert
    suspend fun upsertMany(results: List<PreviousSetResult>): List<Long>

    @Insert
    suspend fun insertMany(result: List<PreviousSetResult>): List<Long>

    @Query("DELETE FROM previousSetResults " +
            "WHERE previously_completed_set_id IN (" +
                "SELECT sr.previously_completed_set_id " +
                "FROM previousSetResults sr " +
                "WHERE sr.workoutId = :workoutId AND " +
                // Delete previous results or current ones in the list
                "(" +
                    "sr.previously_completed_set_id IN (:currentResultsToDelete) OR" +
                    "(sr.mesoCycle != :currentMesocycle OR " +
                    "sr.microCycle != :currentMicrocycle)" +
                ") AND " +
                // Preserve previous results whose lift & position match the current ones being deleted
                "sr.previously_completed_set_id NOT IN (" +
                    "SELECT psr.previously_completed_set_id " +
                    "FROM previousSetResults dsr " +
                    "INNER JOIN previousSetResults psr ON " +
                        "dsr.liftId = psr.liftId AND " +
                        "dsr.liftPosition = psr.liftPosition AND " +
                        "dsr.workoutId = psr.workoutId " +
                    "WHERE dsr.previously_completed_set_id IN (:currentResultsToDelete) AND " +
                    "psr.previously_completed_set_id NOT IN (:currentResultsToDelete)" +
                ")" +
            ")"
    )
    suspend fun deleteAllForPreviousWorkout(
        workoutId: Long,
        currentMesocycle: Int,
        currentMicrocycle: Int,
        currentResultsToDelete: List<Long>,
    ): Int

    @Query("DELETE FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "mesoCycle = :mesoCycle AND " +
            "microCycle = :microCycle")
    suspend fun deleteAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): Int

    @Query("DELETE FROM previousSetResults WHERE previously_completed_set_id = :id")
    suspend fun deleteById(id: Long)
}