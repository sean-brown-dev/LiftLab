package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.data.remote.dto.SyncMetadataDto

interface SyncMetadataRepository {
    suspend fun upsert(syncMetadata: SyncMetadataDto)

    suspend fun get(collectionName: String): SyncMetadataDto?

    suspend fun deleteAll()
}