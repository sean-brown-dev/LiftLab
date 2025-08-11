package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.LiftRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.LiftsDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class LiftsSyncRepository(
    private val liftsDao: LiftsDao,
) : BaseRemoteSyncRepository<LiftRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.LIFTS_COLLECTION
    override val remoteDtoClass: KClass<LiftRemoteDto> = LiftRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<LiftRemoteDto> =
        liftsDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

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

    override suspend fun deleteByRemoteId(remoteId: String): Int {
        val toDelete = liftsDao.getByRemoteId(remoteId) ?: return 0
        return liftsDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = liftsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return liftsDao.deleteMany(toDelete)
    }
}