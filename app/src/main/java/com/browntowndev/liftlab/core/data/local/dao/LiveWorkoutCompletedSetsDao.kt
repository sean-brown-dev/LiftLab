package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.entities.LiveWorkoutCompletedSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveWorkoutCompletedSetsDao: BaseDao<LiveWorkoutCompletedSetEntity> {
    @Query("""
        SELECT * FROM liveWorkoutCompletedSets
        WHERE liftId = :liftId AND 
              liftPosition = :liftPosition AND 
              setPosition = :position AND 
              deleted = 0
    """)
    suspend fun getAllForLiftAtPosition(liftId: Long, liftPosition: Int, position: Int): List<LiveWorkoutCompletedSetEntity>

    @Query("SELECT * FROM liveWorkoutCompletedSets WHERE synced = 0")
    suspend fun getAllUnsynced(): List<LiveWorkoutCompletedSetEntity>

    @Query("SELECT * FROM liveWorkoutCompletedSets WHERE live_workout_completed_set_id = :id AND deleted = 0")
    suspend fun get(id: Long): LiveWorkoutCompletedSetEntity?

    @Transaction
    @Query("SELECT * FROM liveWorkoutCompletedSets WHERE live_workout_completed_set_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<LiveWorkoutCompletedSetEntity>

    @Transaction
    @Query("SELECT * FROM liveWorkoutCompletedSets WHERE deleted = 0")
    suspend fun getAll(): List<LiveWorkoutCompletedSetEntity>

    @Transaction
    @Query("SELECT * FROM liveWorkoutCompletedSets WHERE deleted = 0")
    fun getAllFlow(): Flow<List<LiveWorkoutCompletedSetEntity>>

    @Query("DELETE FROM liveWorkoutCompletedSets")
    suspend fun deleteAll()

    @Query("UPDATE liveWorkoutCompletedSets SET deleted = 1, synced = 0 WHERE workoutId = :workoutId")
    suspend fun softDeleteAllByWorkoutId(workoutId: Long)

    @Query("UPDATE liveWorkoutCompletedSets SET deleted = 1, synced = 0 WHERE workoutId IN (:workoutIds)")
    suspend fun softDeleteByWorkoutIds(workoutIds: List<Long>)

    @Query("UPDATE liveWorkoutCompletedSets SET deleted = 1, synced = 0")
    suspend fun softDeleteAll()

    @Query("UPDATE liveWorkoutCompletedSets SET deleted = 1, synced = 0 WHERE live_workout_completed_set_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE liveWorkoutCompletedSets SET deleted = 1, synced = 0 WHERE live_workout_completed_set_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM liveWorkoutCompletedSets WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): LiveWorkoutCompletedSetEntity?

    @Query("SELECT * FROM liveWorkoutCompletedSets WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<LiveWorkoutCompletedSetEntity>

    @Query("""
        UPDATE liveWorkoutCompletedSets
        SET deleted = 1, synced = 0
        WHERE workoutId IN (
            SELECT workoutId FROM workouts WHERE programId = :programId
        )
    """)
    suspend fun softDeleteByProgramId(programId: Long)

    @Query("""
        UPDATE liveWorkoutCompletedSets
        SET liftId = :newLiftId, synced = 0
        WHERE liftId IN (:existingLiftIds)
    """)
    fun changeFromLiftsToNewLift(newLiftId: Long, existingLiftIds: List<Long>)
}