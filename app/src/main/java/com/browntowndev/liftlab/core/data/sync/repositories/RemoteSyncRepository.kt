package com.browntowndev.liftlab.core.data.sync.repositories

import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import kotlin.reflect.KClass

interface RemoteSyncRepository {
    val collectionName: String
    val remoteDtoType: KClass<out BaseRemoteDto>

    suspend fun getAllUnsynced(): List<BaseRemoteDto>

    suspend fun getManyByRemoteId(remoteIds: List<String>): List<BaseRemoteDto>

    suspend fun upsertMany(entities: List<BaseRemoteDto>): List<Long>
}