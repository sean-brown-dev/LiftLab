package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface RestTimerInProgressDao: BaseDao<RestTimerInProgress> {
    @Query("SELECT * FROM restTimerInProgress")
    fun getAsFlow(): Flow<RestTimerInProgress?>

    @Query("SELECT * FROM restTimerInProgress")
    suspend fun get(): RestTimerInProgress?

    @Query("SELECT * FROM restTimerInProgress")
    suspend fun getAll(): List<RestTimerInProgress>
}