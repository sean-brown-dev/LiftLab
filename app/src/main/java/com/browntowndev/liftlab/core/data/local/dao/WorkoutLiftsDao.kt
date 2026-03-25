package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.data.local.entities.WorkoutLiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutLiftsDao: BaseDao<WorkoutLiftEntity> {
    @Query("SELECT * FROM workoutLifts WHERE synced = 0")
    suspend fun getAllUnsynced(): List<WorkoutLiftEntity>

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id = :id AND deleted = 0")
    suspend fun get(id: Long): WorkoutLiftWithRelationships?

    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id = :id AND deleted = 0")
    suspend fun getWithoutRelationships(id: Long): WorkoutLiftEntity?

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id = :id AND deleted = 0")
    suspend fun getWithRelationships(id: Long): WorkoutLiftWithRelationships?

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<WorkoutLiftWithRelationships>

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id IN (:ids) AND deleted = 0")
    suspend fun getManyWithoutRelationships(ids: List<Long>): List<WorkoutLiftEntity>

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE deleted = 0")
    suspend fun getAll(): List<WorkoutLiftWithRelationships>

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE deleted = 0")
    fun getAllFlow(): Flow<List<WorkoutLiftWithRelationships>>

    @Query("DELETE FROM workoutLifts")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT liftId FROM workoutLifts WHERE workoutId = :workoutId AND deleted = 0")
    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long>

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workoutId = :workoutId AND deleted = 0")
    suspend fun getForWorkout(workoutId: Long): List<WorkoutLiftWithRelationships>

    @Query("UPDATE workoutLifts SET deleted = 1, synced = 0 WHERE workout_lift_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE workoutLifts SET deleted = 1, synced = 0 WHERE workout_lift_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM workoutLifts WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): WorkoutLiftEntity?

    @Query("SELECT * FROM workoutLifts WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<WorkoutLiftEntity>

    @Query("""
        UPDATE workoutLifts
        SET deleted = 1, synced = 0
        WHERE workoutId IN (
            SELECT workout_id FROM workouts WHERE programId = :programId
        )
    """)
    suspend fun softDeleteByProgramId(programId: Long)

    @Query("UPDATE workoutLifts SET deleted = 1, synced = 0 WHERE workoutId = :workoutId")
    suspend fun softDeleteByWorkoutId(workoutId: Long)

    @Query("UPDATE workoutLifts SET deleted = 1, synced = 0 WHERE workoutId IN (:workoutIds)")
    suspend fun softDeleteByWorkoutIds(workoutIds: List<Long>)

    @Query("""
        UPDATE workoutLifts 
        SET liftId = :newLiftId, synced = 0 
        WHERE liftId IN (:existingLiftIds)
    """)
    suspend fun changeFromLiftsToNewLift(newLiftId: Long, existingLiftIds: List<Long>)
}