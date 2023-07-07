package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.entities.PreviousSetResult

@Dao
interface PreviousSetResultDao {
    @Query("SELECT * FROM previousSetResults WHERE workoutId = :workoutId")
    suspend fun getByWorkoutId(workoutId: Long): List<PreviousSetResult>

    @Insert
    suspend fun insert(result: PreviousSetResult): Long

    @Query("DELETE FROM previousSetResults " +
            "WHERE workoutId = :workoutId AND " +
            "(mesoCycle != :mesoCycle OR microCycle != :microCycle)")
    suspend fun deleteAllNotForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): Int

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
    suspend fun delete(workoutId: Long, liftId: Long, setPosition: Int, myoRepSetPosition: Int)
}