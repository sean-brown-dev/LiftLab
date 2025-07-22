package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.CustomLiftSetRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.CustomSetsDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.reflect.KClass

class CustomSetsSyncRepository(
    private val customSetsDao: CustomSetsDao,
) : BaseRemoteSyncRepository<CustomLiftSetRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.CUSTOM_LIFT_SETS_COLLECTION
    override val remoteDtoClass: KClass<CustomLiftSetRemoteDto> = CustomLiftSetRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<CustomLiftSetRemoteDto> {
        TODO("Not yet implemented")
    }

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
}