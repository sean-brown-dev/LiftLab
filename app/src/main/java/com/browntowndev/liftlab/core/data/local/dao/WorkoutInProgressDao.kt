package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.browntowndev.liftlab.core.data.local.entities.WorkoutInProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutInProgressDao: BaseDao<WorkoutInProgressEntity> {
    @Query("SELECT * FROM workoutsInProgress WHERE workout_in_progress_id = 1 AND synced = 0")
    suspend fun getAllUnsynced(): List<WorkoutInProgressEntity>

    @Query("SELECT * FROM workoutsInProgress WHERE workout_in_progress_id = 1 AND deleted = 0")
    suspend fun get(): WorkoutInProgressEntity?

    @Query("SELECT * FROM workoutsInProgress WHERE workout_in_progress_id = 1 AND deleted = 0")
    fun getFlow(): Flow<WorkoutInProgressEntity?>

    @Query("DELETE FROM workoutsInProgress WHERE workout_in_progress_id = 1")
    suspend fun delete(): Int

    @Query("UPDATE workoutsInProgress SET deleted = 1, synced = 0 WHERE workout_in_progress_id = 1")
    suspend fun softDelete(): Int

    @Query("UPDATE workoutsInProgress SET deleted = 1, synced = 0 WHERE workoutId IN (:workoutIds)")
    fun softDeleteByWorkoutIds(workoutIds: List<Long>): Int

    @Query("""
        UPDATE workoutsInProgress 
        SET deleted = 1, synced = 0 
        WHERE workoutId IN (
            SELECT workoutId FROM workouts WHERE programId = :programId
        )
    """)
    suspend fun softDeleteByProgramId(programId: Long): Int
}