package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
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
                    "WHERE dsr.previously_completed_set_id IN (:currentResultsToDelete)" +
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

    @Query("DELETE FROM previousSetResults WHERE " +
            "workoutId = :workoutId AND " +
            "liftId = :liftId AND " +
            "setPosition = :setPosition")
    suspend fun delete(workoutId: Long, liftId: Long, setPosition: Int)

    @Query("DELETE FROM previousSetResults WHERE " +
            "workoutId = :workoutId AND " +
            "liftPosition = :liftPosition AND " +
            "setPosition = :setPosition AND " +
            "myoRepSetPosition = :myoRepSetPosition")
    suspend fun delete(workoutId: Long, liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?)

    @Query("UPDATE previousSetResults " +
            "SET weight = :weight, " +
            "reps = :reps, " +
            "rpe = :rpe " +
            "WHERE liftId = :liftId AND " +
            "liftPosition = :liftPosition AND " +
            "setPosition = :setPosition AND " +
            "myoRepSetPosition = :myoRepSetPosition")
    suspend fun update(liftId: Long, liftPosition: Int, setPosition: Int, myoRepSetPosition: Int?, weight: Float, reps: Int, rpe: Float)
}