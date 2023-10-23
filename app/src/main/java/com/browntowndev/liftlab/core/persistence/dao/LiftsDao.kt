package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.persistence.entities.Lift
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

@Dao
interface LiftsDao {
    @Insert
    suspend fun insertAll(items: List<Lift>): List<Long>

    @Insert
    suspend fun insert(lift: Lift)

    @Update
    suspend fun update(lift: Lift)

    @Query("UPDATE lifts SET isHidden = 0")
    suspend fun show()

    @Query("SELECT * FROM lifts WHERE lift_id = :id")
    suspend fun get(id: Long): Lift

    @Query("SELECT * FROM lifts WHERE isHidden = 0")
    fun getAllAsFlow(): Flow<List<Lift>>

    @Query("SELECT * FROM lifts WHERE isHidden = 0")
    suspend fun getAll(): List<Lift>

    @Query("SELECT * FROM lifts WHERE movementPattern = :movementPattern")
    suspend fun getByCategory(movementPattern: MovementPattern): List<Lift>

    @Query("UPDATE lifts SET restTime = :newRestTime, restTimerEnabled = :enabled WHERE lift_id = :id")
    suspend fun updateRestTime(id: Long, enabled: Boolean, newRestTime: Duration?)

    @Query("UPDATE lifts SET incrementOverride = :newIncrementOverride WHERE lift_id = :id")
    suspend fun updateIncrementOverride(id: Long, newIncrementOverride: Float?)
}