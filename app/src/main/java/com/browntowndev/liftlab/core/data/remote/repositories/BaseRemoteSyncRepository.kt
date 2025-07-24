package com.browntowndev.liftlab.core.data.remote.repositories

import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import kotlin.reflect.KClass

abstract class BaseRemoteSyncRepository<D: BaseRemoteDto>: RemoteSyncRepository {
    abstract val remoteDtoClass: KClass<D>

    override val remoteDtoType: KClass<out BaseRemoteDto>
        get() = remoteDtoClass

    // Shims for generic to implementing types
    final override suspend fun getManyByRemoteId(remoteIds: List<String>): List<BaseRemoteDto> =
        getManyByRemoteIdTyped(remoteIds)

    final override suspend fun getAllUnsynced(): List<BaseRemoteDto> =
        getAllUnsyncedTyped()

    final override suspend fun upsertMany(entities: List<BaseRemoteDto>): List<Long> {
        val specificEntities = entities.mapNotNull { remoteDtoClass.java.cast(it) }
        if (specificEntities.size != entities.size) {
            error("Type mismatch during upsert for ${collectionName}. Expected all entities to be of type ${remoteDtoClass.simpleName}.")
        }
        return upsertManyTyped(specificEntities)
    }

    protected abstract suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<D>

    protected abstract suspend fun getAllUnsyncedTyped(): List<D>

    protected abstract suspend fun upsertManyTyped(entities: List<D>): List<Long>
}