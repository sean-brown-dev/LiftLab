package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.PreviousSetResultRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class PreviousSetResultsSyncRepository(
    private val previousSetResultsDao: PreviousSetResultDao
) : BaseRemoteSyncRepository<PreviousSetResultRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.PREVIOUS_SET_RESULTS_COLLECTION
    override val remoteDtoClass: KClass<PreviousSetResultRemoteDto> = PreviousSetResultRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<PreviousSetResultRemoteDto> =
        previousSetResultsDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<PreviousSetResultRemoteDto> =
        previousSetResultsDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<PreviousSetResultRemoteDto>): List<Long> =
        previousSetResultsDao.upsertMany(entities.fastMap { it.toEntity() })
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
        val toDelete = previousSetResultsDao.getByRemoteId(remoteId) ?: return 0
        return previousSetResultsDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = previousSetResultsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return previousSetResultsDao.deleteMany(toDelete)
    }
}