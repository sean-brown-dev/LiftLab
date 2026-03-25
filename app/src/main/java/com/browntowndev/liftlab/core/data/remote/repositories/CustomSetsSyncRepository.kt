package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.CustomLiftSetRemoteDto
import com.browntowndev.liftlab.core.sync.RemoteCollectionNames
import kotlin.reflect.KClass

class CustomSetsSyncRepository(
    private val customSetsDao: CustomSetsDao,
) : BaseRemoteSyncRepository<CustomLiftSetRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.CUSTOM_LIFT_SETS_COLLECTION
    override val remoteDtoClass: KClass<CustomLiftSetRemoteDto> = CustomLiftSetRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<CustomLiftSetRemoteDto> =
        customSetsDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<CustomLiftSetRemoteDto> =
        customSetsDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<CustomLiftSetRemoteDto>): List<Long> =
        customSetsDao.upsertMany(entities.fastMap { it.toEntity() })
            .let { upsertIds ->
                entities.zip(upsertIds).fastMap { (entity, id) ->
                    if (id == -1L) {
                        entity.id
                    } else {
                        id
                    }
                }
            }

    override suspend fun deleteByRemoteId(remoteId: String): Int {
        val toDelete = customSetsDao.getByRemoteId(remoteId) ?: return 0
        return customSetsDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = customSetsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return customSetsDao.deleteMany(toDelete)
    }
}