package com.browntowndev.liftlab.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.common.enums.LiftCategory
import com.browntowndev.liftlab.core.data.dtos.LiftDto
import com.browntowndev.liftlab.core.data.entities.Lift

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

    @Query("SELECT * FROM lifts")
    suspend fun getAll(): List<LiftDto>

    @Query("SELECT * FROM lifts WHERE category = :category")
    suspend fun getByCategory(category: LiftCategory): List<LiftDto>
}