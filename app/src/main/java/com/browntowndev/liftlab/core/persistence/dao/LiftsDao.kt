package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.persistence.entities.Lift
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

@Dao
interface LiftsDao: BaseDao<Lift> {
    @Query("DELETE FROM lifts")
    suspend fun deleteAll()

    @Query("UPDATE lifts SET isHidden = 0")
    suspend fun show()

    @Query("SELECT * FROM lifts WHERE lift_id = :id")
    suspend fun get(id: Long): Lift?

    @Transaction
    @Query("SELECT * FROM lifts WHERE lift_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<Lift>

    @Transaction
    @Query("SELECT * FROM lifts WHERE isHidden = 0")
    fun getAllAsFlow(): Flow<List<Lift>>

    @Transaction
    @Query("SELECT * FROM lifts WHERE isHidden = :includeHidden")
    suspend fun getAll(includeHidden: Boolean = false): List<Lift>

    @Transaction
    @Query("SELECT * FROM lifts WHERE movementPattern = :movementPattern")
    suspend fun getByCategory(movementPattern: MovementPattern): List<Lift>

    @Query("UPDATE lifts SET restTime = :newRestTime, restTimerEnabled = :enabled, synced = 0 WHERE lift_id = :id")
    suspend fun updateRestTime(id: Long, enabled: Boolean, newRestTime: Duration?)

    @Query("UPDATE lifts SET incrementOverride = :newIncrementOverride, synced = 0 WHERE lift_id = :id")
    suspend fun updateIncrementOverride(id: Long, newIncrementOverride: Float?)

    @Query("UPDATE lifts SET note = :note, synced = 0 WHERE lift_id = :id")
    suspend fun updateNote(id: Long, note: String?)
}