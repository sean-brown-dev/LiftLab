package com.browntowndev.liftlab.core.domain.repositories.sync

import com.browntowndev.liftlab.core.persistence.room.dao.SyncDao
import com.browntowndev.liftlab.core.persistence.firestore.entities.SyncMetadataDto
import com.browntowndev.liftlab.core.persistence.entities.room.SyncMetadataEntity

class SyncMetadataRepository(private val dao: SyncDao) {
    suspend fun upsert(syncMetadata: SyncMetadataDto) {
        dao.upsert(
            SyncMetadataEntity(
                collectionName = syncMetadata.collectionName,
                lastSyncTimestamp = syncMetadata.lastSyncTimestamp,
            )
        )
    }

    suspend fun get(collectionName: String): SyncMetadataDto? {
        return dao.getForCollection(collectionName)?.let {
            SyncMetadataDto(
                collectionName = it.collectionName,
                lastSyncTimestamp = it.lastSyncTimestamp
            )
        }
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}