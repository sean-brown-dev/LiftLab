package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.SyncDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.SyncMetadataDto
import com.browntowndev.liftlab.core.persistence.entities.SyncMetadata

class SyncMetadataRepository(private val dao: SyncDao) {
    suspend fun upsert(syncMetadata: SyncMetadataDto) {
        dao.upsert(
            SyncMetadata(
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
}