package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.dtos.queryable.ProgramWithRelationships
import com.browntowndev.liftlab.core.persistence.entities.Program

@Dao
interface ProgramsDao {
    @Insert
    suspend fun insert(program: Program): Long

    @Delete
    suspend fun delete(program: Program)

    @Query("DELETE FROM programs WHERE program_id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM programs WHERE isActive = 1")
    suspend fun getActive(): ProgramWithRelationships?

    @Transaction
    @Query("SELECT * FROM programs WHERE program_id = :id")
    suspend fun get(id: Long) : ProgramWithRelationships

    @Query("UPDATE programs SET name = :newName WHERE program_id = :id")
    suspend fun updateName(id: Long, newName: String)
}