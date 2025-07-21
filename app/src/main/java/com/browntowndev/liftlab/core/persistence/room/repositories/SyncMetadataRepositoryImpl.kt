package com.browntowndev.liftlab.core.persistence.room.repositories

import com.browntowndev.liftlab.core.domain.repositories.SyncMetadataRepository
import com.browntowndev.liftlab.core.persistence.firestore.documents.SyncMetadataDto
import com.browntowndev.liftlab.core.persistence.room.dao.SyncDao
import com.browntowndev.liftlab.core.persistence.room.entities.SyncMetadataEntity

class SyncMetadataRepositoryImpl(private val dao: SyncDao): SyncMetadataRepository {
    override suspend fun upsert(syncMetadata: SyncMetadataDto) {
        dao.upsert(
            SyncMetadataEntity(
                collectionName = syncMetadata.collectionName,
                lastSyncTimestamp = syncMetadata.lastSyncTimestamp,
            )
        )
    }

    override suspend fun get(collectionName: String): SyncMetadataDto? {
        return dao.getForCollection(collectionName)?.let {
            SyncMetadataDto(
                collectionName = it.collectionName,
                lastSyncTimestamp = it.lastSyncTimestamp
            )
        }
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}