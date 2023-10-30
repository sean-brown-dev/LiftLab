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
            "SELECT previously_completed_set_id " +
            "FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "(mesoCycle != :mesoCycle OR " +
            "microCycle != :microCycle))")
    suspend fun deleteAllForPreviousWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): Int

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
            "liftId = :liftId AND " +
            "setPosition = :setPosition AND " +
            "myoRepSetPosition = :myoRepSetPosition")
    suspend fun delete(workoutId: Long, liftId: Long, setPosition: Int, myoRepSetPosition: Int?)
}