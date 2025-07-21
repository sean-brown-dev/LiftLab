package com.browntowndev.liftlab.core.persistence.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.room.dtos.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.room.WorkoutLiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutLiftsDao: BaseDao<WorkoutLiftEntity> {
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id = :id")
    suspend fun get(id: Long): WorkoutLiftEntity?

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id = :id")
    suspend fun getWithRelationships(id: Long): WorkoutLiftWithRelationships?

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<WorkoutLiftEntity>

    @Transaction
    @Query("SELECT * FROM workoutLifts")
    suspend fun getAll(): List<WorkoutLiftEntity>

    @Transaction
    @Query("SELECT * FROM workoutLifts")
    fun getAllFlow(): Flow<List<WorkoutLiftEntity>>

    @Query("DELETE FROM workoutLifts")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT liftId FROM workoutLifts WHERE workoutId = :workoutId")
    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long>

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workoutId = :workoutId")
    suspend fun getForWorkout(workoutId: Long): List<WorkoutLiftWithRelationships>
}