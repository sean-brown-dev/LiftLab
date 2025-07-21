package com.browntowndev.liftlab.core.persistence.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.entities.room.RestTimerInProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestTimerInProgressDao: BaseDao<RestTimerInProgressEntity> {
    @Query("SELECT * FROM restTimerInProgress")
    fun getAsFlow(): Flow<RestTimerInProgressEntity?>

    @Query("SELECT * FROM restTimerInProgress")
    suspend fun get(): RestTimerInProgressEntity?

    @Query("SELECT * FROM restTimerInProgress")
    suspend fun getAll(): List<RestTimerInProgressEntity>

    @Query("DELETE FROM restTimerInProgress")
    suspend fun deleteAll()
}