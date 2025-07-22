package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.data.local.entities.SyncMetadataEntity

@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM syncMetadata WHERE collectionName = :collectionName")
    suspend fun getForCollection(collectionName: String): SyncMetadataEntity?

    @Upsert
    suspend fun upsert(syncMetadataEntity: SyncMetadataEntity)

    @Query("DELETE FROM syncMetadata")
    suspend fun deleteAll()
}