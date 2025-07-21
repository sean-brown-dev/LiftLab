package com.browntowndev.liftlab.core.persistence.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.entities.room.SyncMetadataEntity

@Dao
interface SyncDao {
    @Query("SELECT * FROM syncMetadata WHERE collectionName = :collectionName")
    suspend fun getForCollection(collectionName: String): SyncMetadataEntity?

    @Upsert
    suspend fun upsert(syncMetadataEntity: SyncMetadataEntity)

    @Query("DELETE FROM syncMetadata")
    suspend fun deleteAll()
}