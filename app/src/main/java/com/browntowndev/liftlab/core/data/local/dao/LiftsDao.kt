package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiftsDao: BaseDao<LiftEntity> {
    @Query("SELECT * FROM lifts WHERE synced = 0")
    suspend fun getAllUnsynced(): List<LiftEntity>

    @Query("DELETE FROM lifts")
    suspend fun deleteAll()

    @Query("SELECT * FROM lifts WHERE lift_id = :id AND deleted = 0")
    suspend fun get(id: Long): LiftEntity?

    @Transaction
    @Query("SELECT * FROM lifts WHERE lift_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<LiftEntity>

    @Transaction
    @Query("SELECT * FROM lifts WHERE deleted = 0")
    fun getAllAsFlow(): Flow<List<LiftEntity>>

    @Transaction
    @Query("SELECT * FROM lifts WHERE deleted = 0")
    suspend fun getAll(): List<LiftEntity>

    @Transaction
    @Query("SELECT * FROM lifts WHERE movementPattern = :movementPattern AND deleted = 0")
    suspend fun getByCategory(movementPattern: MovementPattern): List<LiftEntity>

    @Query("UPDATE lifts SET deleted = 1, synced = 0 WHERE lift_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE lifts SET deleted = 1, synced = 0 WHERE lift_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM lifts WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): LiftEntity?

    @Query("SELECT * FROM lifts WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<LiftEntity>
}