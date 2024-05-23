package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.ProgramWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Program
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgramsDao {
    @Insert
    suspend fun insert(program: Program): Long

    @Delete
    suspend fun delete(program: Program)

    @Update
    suspend fun update(program: Program)

    @Transaction
    @Update
    suspend fun updateMany(programs: List<Program>)

    @Query("DELETE FROM programs WHERE program_id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1")
    fun getActive(): Flow<ProgramWithRelationships?>

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1")
    fun getActiveNotAsLiveData(): ProgramWithRelationships?

    @Transaction
    @Query("SELECT * FROM programs")
    suspend fun getAll(): List<Program>

    @Transaction
    @Query("SELECT * FROM programs WHERE program_id = :id")
    suspend fun get(id: Long) : ProgramWithRelationships

    @Query("UPDATE programs SET name = :newName WHERE program_id = :id")
    suspend fun updateName(id: Long, newName: String)

    @Query("UPDATE programs SET deloadWeek = :newDeloadWeek WHERE program_id = :id")
    suspend fun updateDeloadWeek(id: Long, newDeloadWeek: Int)

    @Query("SELECT deloadWeek FROM programs WHERE program_id = :id")
    suspend fun getDeloadWeek(id: Long): Int

    @Query("SELECT program_id AS programId, name, deloadWeek, currentMesocycle, currentMicrocycle, currentMicrocyclePosition, " +
            "(SELECT COUNT(*) FROM workouts WHERE programId = program_id) AS workoutCount " +
            "FROM programs " +
            "WHERE isActive = 1")
    fun getActiveProgramMetadata(): Flow<ActiveProgramMetadataDto?>

    @Query("UPDATE programs " +
            "SET currentMesocycle = :mesoCycle, " +
            "currentMicroCycle = :microCycle, " +
            "currentMicrocyclePosition = :microCyclePosition " +
            "WHERE program_id = :id")
    suspend fun updateMesoAndMicroCycle(id: Long, mesoCycle: Int, microCycle: Int, microCyclePosition: Int)
}