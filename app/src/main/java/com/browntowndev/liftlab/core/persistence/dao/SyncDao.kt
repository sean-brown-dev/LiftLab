package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.entities.SyncMetadata

@Dao
interface SyncDao {
    @Query("SELECT * FROM syncMetadata WHERE collectionName = :collectionName")
    suspend fun getForCollection(collectionName: String): SyncMetadata?

    @Upsert
    suspend fun upsert(syncMetadata: SyncMetadata)

    @Query("DELETE FROM syncMetadata")
    suspend fun deleteAll()
}