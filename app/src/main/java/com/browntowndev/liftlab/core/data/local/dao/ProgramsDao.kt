package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.dtos.ProgramWithRelationshipsDto
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import com.browntowndev.liftlab.core.data.local.dtos.ProgramMetadataDto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramsDao: BaseDao<ProgramEntity> {
    @Query("SELECT * FROM programs WHERE synced = 0")
    suspend fun getAllUnsynced(): List<ProgramEntity>

    @Query("DELETE FROM programs")
    suspend fun deleteAll()

    @Query("DELETE FROM programs WHERE program_id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1")
    fun getActiveWithRelationshipsFlow(): Flow<ProgramWithRelationshipsDto?>

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1")
    suspend fun getActiveWithRelationships(): ProgramWithRelationshipsDto?

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1")
    suspend fun getAllActive(): List<ProgramEntity>

    @Transaction
    @Query("SELECT * FROM programs")
    suspend fun getAll(): List<ProgramEntity>

    @Transaction
    @Query("SELECT * FROM programs")
    fun getAllWithRelationshipsFlow(): Flow<List<ProgramWithRelationshipsDto>>

    @Query("SELECT * FROM programs WHERE program_id = :id")
    suspend fun get(id: Long) : ProgramEntity?

    @Transaction
    @Query("SELECT * FROM programs WHERE program_id IN (:ids)")
    suspend fun getMany(ids: List<Long>) : List<ProgramEntity>

    @Transaction
    @Query("SELECT * FROM programs WHERE program_id = :id")
    suspend fun getWithRelationships(id: Long) : ProgramWithRelationshipsDto?

    @Query("UPDATE programs SET deloadWeek = :newDeloadWeek WHERE program_id = :id")
    suspend fun updateDeloadWeek(id: Long, newDeloadWeek: Int)

    @Query("SELECT deloadWeek FROM programs WHERE program_id = :id")
    suspend fun getDeloadWeek(id: Long): Int

    @Transaction
    @Query("SELECT program_id AS programId, name, deloadWeek, currentMesocycle, currentMicrocycle, currentMicrocyclePosition, " +
            "(SELECT COUNT(*) FROM workouts WHERE programId = program_id) AS workoutCount " +
            "FROM programs " +
            "WHERE isActive = 1")
    fun getActiveProgramMetadata(): Flow<ProgramMetadataDto?>

    @Query("UPDATE programs SET deleted = 1, synced = 0 WHERE program_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE programs SET deleted = 1, synced = 0 WHERE program_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM programs WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): ProgramEntity?

    @Query("SELECT * FROM programs WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<ProgramEntity>
}