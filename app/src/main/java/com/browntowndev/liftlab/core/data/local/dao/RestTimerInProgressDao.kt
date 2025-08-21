package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.data.local.entities.RestTimerInProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RestTimerInProgressDao {
    @Query("SELECT * FROM restTimerInProgress WHERE rest_timer_in_progress_id = 1")
    fun getAsFlow(): Flow<RestTimerInProgressEntity?>

    @Query("SELECT * FROM restTimerInProgress WHERE rest_timer_in_progress_id = 1")
    suspend fun get(): RestTimerInProgressEntity?

    @Upsert
    suspend fun upsert(restTimerInProgressEntity: RestTimerInProgressEntity): Long

    @Query("DELETE FROM restTimerInProgress WHERE rest_timer_in_progress_id = 1")
    suspend fun delete()
}