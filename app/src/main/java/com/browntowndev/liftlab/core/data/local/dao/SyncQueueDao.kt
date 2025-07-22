package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.browntowndev.liftlab.core.data.local.entities.SyncQueueEntity

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue")
    fun getAll(): List<SyncQueueEntity>

    @Insert
    fun insert(syncQueueEntity: SyncQueueEntity): Long

    @Insert
    fun insertMany(syncQueueEntities: List<SyncQueueEntity>): List<Long>

    @Query("DELETE FROM sync_queue WHERE sync_queue_id IN (:ids)")
    fun deleteMany(ids: List<Long>): Int
}
