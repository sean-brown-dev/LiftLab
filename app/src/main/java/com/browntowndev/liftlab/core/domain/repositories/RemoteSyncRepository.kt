package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.sync.SyncDto
import kotlin.reflect.KClass

interface RemoteSyncRepository {
    val collectionName: String
    val remoteDtoType: KClass<out SyncDto>

    suspend fun getAllUnsynced(): List<SyncDto>

    suspend fun getManyByRemoteId(remoteIds: List<String>): List<SyncDto>

    suspend fun upsertMany(entities: List<SyncDto>): List<Long>

    suspend fun deleteByRemoteId(remoteId: String): Int

    suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int
}