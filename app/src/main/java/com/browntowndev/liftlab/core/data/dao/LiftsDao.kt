package com.browntowndev.liftlab.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.data.dtos.LiftDTO
import com.browntowndev.liftlab.core.data.entities.Lift

@Dao
interface LiftsDao {
    @Insert
    suspend fun insertAll(items: List<Lift>)

    @Insert
    suspend fun insert(lift: Lift)

    @Query("SELECT * FROM lifts")
    suspend fun getAll(): List<LiftDTO>
}