package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.LiftRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.LiftsDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class LiftsSyncRepository(
    private val liftsDao: LiftsDao,
) : BaseRemoteSyncRepository<LiftRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.LIFTS_COLLECTION
    override val remoteDtoClass: KClass<LiftRemoteDto> = LiftRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<LiftRemoteDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllUnsyncedTyped(): List<LiftRemoteDto> =
        liftsDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<LiftRemoteDto>): List<Long> =
        liftsDao.upsertMany(entities.fastMap { it.toEntity() })
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