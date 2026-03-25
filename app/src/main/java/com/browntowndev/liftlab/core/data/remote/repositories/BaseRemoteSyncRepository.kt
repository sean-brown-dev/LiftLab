package com.browntowndev.liftlab.core.data.remote.repositories

import com.browntowndev.liftlab.core.domain.models.sync.SyncDto
import com.browntowndev.liftlab.core.domain.repositories.RemoteSyncRepository
import kotlin.reflect.KClass

abstract class BaseRemoteSyncRepository<D: SyncDto>: RemoteSyncRepository {
    abstract val remoteDtoClass: KClass<D>

    override val remoteDtoType: KClass<out SyncDto>
        get() = remoteDtoClass

    // Shims for generic to implementing types
    final override suspend fun getManyByRemoteId(remoteIds: List<String>): List<SyncDto> =
        getManyByRemoteIdTyped(remoteIds)

    final override suspend fun getAllUnsynced(): List<SyncDto> =
        getAllUnsyncedTyped()

    final override suspend fun upsertMany(entities: List<SyncDto>): List<Long> {
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