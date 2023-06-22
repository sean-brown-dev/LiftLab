package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.persistence.entities.Lift

@Dao
interface LiftsDao {
    @Insert
    suspend fun insertAll(items: List<Lift>): List<Long>

    @Insert
    suspend fun insert(lift: Lift)

    @Query("UPDATE lifts SET isHidden = 1")
    suspend fun hide()

    @Query("UPDATE lifts SET isHidden = 0")
    suspend fun show()

    @Query("SELECT * FROM lifts WHERE lift_id = :id")
    suspend fun get(id: Long): Lift

    @Query("SELECT * FROM lifts")
    suspend fun getAll(): List<Lift>

    @Query("SELECT * FROM lifts WHERE movementPattern = :movementPattern")
    suspend fun getByCategory(movementPattern: MovementPattern): List<Lift>
}