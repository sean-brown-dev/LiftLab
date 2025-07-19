package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.dtos.queryable.WorkoutLiftWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLift
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutLiftsDao: BaseDao<WorkoutLift> {
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id = :id")
    suspend fun get(id: Long): WorkoutLift?

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id = :id")
    suspend fun getWithRelationships(id: Long): WorkoutLiftWithRelationships?

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workout_lift_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<WorkoutLift>

    @Transaction
    @Query("SELECT * FROM workoutLifts")
    suspend fun getAll(): List<WorkoutLift>

    @Transaction
    @Query("SELECT * FROM workoutLifts")
    fun getAllFlow(): Flow<List<WorkoutLift>>

    @Query("DELETE FROM workoutLifts")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT liftId FROM workoutLifts WHERE workoutId = :workoutId")
    suspend fun getLiftIdsForWorkout(workoutId: Long): List<Long>

    @Transaction
    @Query("SELECT * FROM workoutLifts WHERE workoutId = :workoutId")
    suspend fun getForWorkout(workoutId: Long): List<WorkoutLiftWithRelationships>
}