package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.ProgramWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Program
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramsDao: BaseDao<Program> {
    @Query("DELETE FROM programs")
    suspend fun deleteAll()

    @Query("DELETE FROM programs WHERE program_id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1")
    fun getActive(): Flow<ProgramWithRelationships?>

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1")
    suspend fun getActiveNotAsLiveData(): ProgramWithRelationships?

    @Transaction
    @Query("SELECT * FROM programs")
    suspend fun getAll(): List<Program>

    @Query("SELECT * FROM programs WHERE program_id = :id")
    suspend fun get(id: Long) : Program?

    @Transaction
    @Query("SELECT * FROM programs WHERE program_id IN (:ids)")
    suspend fun getMany(ids: List<Long>) : List<Program>

    @Transaction
    @Query("SELECT * FROM programs WHERE program_id = :id")
    suspend fun getWithRelationships(id: Long) : ProgramWithRelationships

    @Query("UPDATE programs SET deloadWeek = :newDeloadWeek WHERE program_id = :id")
    suspend fun updateDeloadWeek(id: Long, newDeloadWeek: Int)

    @Query("SELECT deloadWeek FROM programs WHERE program_id = :id")
    suspend fun getDeloadWeek(id: Long): Int

    @Transaction
    @Query("SELECT program_id AS programId, name, deloadWeek, currentMesocycle, currentMicrocycle, currentMicrocyclePosition, " +
            "(SELECT COUNT(*) FROM workouts WHERE programId = program_id) AS workoutCount " +
            "FROM programs " +
            "WHERE isActive = 1")
    fun getActiveProgramMetadata(): Flow<ActiveProgramMetadataDto?>
}