package com.browntowndev.liftlab.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.dtos.ProgramDto
import com.browntowndev.liftlab.core.data.entities.Program

@Dao
interface ProgramsDao {
    @Insert
    suspend fun insert(program: Program): Long

    @Delete
    suspend fun delete(program: Program)

    @Query("DELETE FROM programs WHERE program_id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM programs")
    suspend fun getAll(): List<ProgramDto>

    @Transaction
    @Query("SELECT * FROM programs WHERE program_id = :id")
    suspend fun get(id: Long) : ProgramDto

    @Query("UPDATE programs SET name = :newName WHERE program_id = :id")
    suspend fun updateName(id: Long, newName: String)
}