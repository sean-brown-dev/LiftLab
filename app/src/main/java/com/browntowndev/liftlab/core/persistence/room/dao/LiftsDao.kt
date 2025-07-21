package com.browntowndev.liftlab.core.persistence.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.persistence.room.entities.LiftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiftsDao: BaseDao<LiftEntity> {
    @Query("DELETE FROM lifts")
    suspend fun deleteAll()

    @Query("UPDATE lifts SET isHidden = 0")
    suspend fun show()

    @Query("SELECT * FROM lifts WHERE lift_id = :id")
    suspend fun get(id: Long): LiftEntity?

    @Transaction
    @Query("SELECT * FROM lifts WHERE lift_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<LiftEntity>

    @Transaction
    @Query("SELECT * FROM lifts WHERE isHidden = 0")
    fun getAllAsFlow(): Flow<List<LiftEntity>>

    @Transaction
    @Query("SELECT * FROM lifts WHERE isHidden == 0 OR isHidden = :includeHidden")
    suspend fun getAll(includeHidden: Boolean = false): List<LiftEntity>

    @Transaction
    @Query("SELECT * FROM lifts WHERE movementPattern = :movementPattern")
    suspend fun getByCategory(movementPattern: MovementPattern): List<LiftEntity>
}