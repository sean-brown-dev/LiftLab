package com.browntowndev.liftlab.core.data.repositories

import com.browntowndev.liftlab.core.domain.repositories.SyncMetadataRepository
import com.browntowndev.liftlab.core.data.remote.dto.SyncMetadataDto
import com.browntowndev.liftlab.core.data.local.dao.SyncMetadataDao
import com.browntowndev.liftlab.core.data.local.entities.SyncMetadataEntity

class SyncMetadataRepositoryImpl(private val dao: SyncMetadataDao): SyncMetadataRepository {
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