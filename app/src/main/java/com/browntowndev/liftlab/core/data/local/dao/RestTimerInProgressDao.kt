package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.browntowndev.liftlab.core.data.local.entities.RestTimerInProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestTimerInProgressDao: BaseDao<RestTimerInProgressEntity> {
    @Query("SELECT * FROM restTimerInProgress")
    fun getAsFlow(): Flow<RestTimerInProgressEntity?>

    @Query("SELECT * FROM restTimerInProgress WHERE rest_timer_in_progress_id = :id")
    suspend fun getById(id: Long): RestTimerInProgressEntity?

    @Query("SELECT * FROM restTimerInProgress WHERE rest_timer_in_progress_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<RestTimerInProgressEntity>

    @Query("SELECT * FROM restTimerInProgress")
    suspend fun get(): RestTimerInProgressEntity?

    @Query("SELECT * FROM restTimerInProgress")
    suspend fun getAll(): List<RestTimerInProgressEntity>

    @Query("SELECT * FROM restTimerInProgress")
    fun getAllFlow(): Flow<List<RestTimerInProgressEntity>>

    @Query("DELETE FROM restTimerInProgress")
    suspend fun deleteAll()
}