package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.entities.CustomLiftSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSetsDao: BaseDao<CustomLiftSetEntity> {
    @Query("SELECT * FROM sets WHERE synced = 0")
    suspend fun getAllUnsynced(): List<CustomLiftSetEntity>

    @Transaction
    @Query("SELECT * FROM sets WHERE set_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<CustomLiftSetEntity>

    @Transaction
    @Query("SELECT * FROM sets WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<CustomLiftSetEntity>

    @Query("SELECT * FROM sets WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): CustomLiftSetEntity?

    @Query("SELECT * FROM sets WHERE set_id = :id AND deleted = 0")
    suspend fun get(id: Long): CustomLiftSetEntity?

    @Transaction
    @Query("SELECT * FROM sets WHERE deleted = 0")
    suspend fun getAll(): List<CustomLiftSetEntity>

    @Transaction
    @Query("SELECT * FROM sets WHERE deleted = 0")
    fun getAllFlow(): Flow<List<CustomLiftSetEntity>>

    @Query("DELETE FROM sets")
    suspend fun deleteAll()

    @Query("UPDATE sets SET position = position - 1 WHERE workoutLiftId = :workoutLiftId AND position > :afterPosition")
    suspend fun syncPositions(workoutLiftId: Long, afterPosition: Int)

    @Query("SELECT * FROM sets WHERE workoutLiftId = :workoutLiftId AND deleted = 0")
    suspend fun getByWorkoutLiftId(workoutLiftId: Long): List<CustomLiftSetEntity>

    @Query("UPDATE sets SET deleted = 1, synced = 0 WHERE set_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE sets SET deleted = 1, synced = 0 WHERE set_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("""
        UPDATE sets
        SET deleted = 1, synced = 0
        WHERE workoutLiftId IN (
            SELECT workout_lift_id
            FROM workoutLifts wl
            INNER JOIN workouts w ON w.workout_id = wl.workoutId
            WHERE w.programId = :programId
        )
    """)
    fun softDeleteByProgramId(programId: Long)
}