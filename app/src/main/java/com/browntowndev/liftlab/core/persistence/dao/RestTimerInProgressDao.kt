package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface RestTimerInProgressDao {
    @Insert
    suspend fun insert(entity: RestTimerInProgress)

    @Query("DELETE FROM restTimerInProgress")
    suspend fun deleteAll()

    @Query("SELECT * FROM restTimerInProgress")
    fun getAsFlow(): Flow<RestTimerInProgress?>

    @Query("SELECT * FROM restTimerInProgress")
    suspend fun get(): RestTimerInProgress?
}