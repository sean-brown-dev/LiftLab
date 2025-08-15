package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.SetLogEntryRemoteDto
import com.browntowndev.liftlab.core.sync.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class SetLogEntriesSyncRepository(
    private val setLogEntryDao: SetLogEntryDao,
) : BaseRemoteSyncRepository<SetLogEntryRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.SET_LOG_ENTRIES_COLLECTION
    override val remoteDtoClass: KClass<SetLogEntryRemoteDto> = SetLogEntryRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<SetLogEntryRemoteDto> =
        setLogEntryDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<SetLogEntryRemoteDto> =
        setLogEntryDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<SetLogEntryRemoteDto>): List<Long> =
        setLogEntryDao.upsertMany(entities.fastMap { it.toEntity() })
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
        val toDelete = setLogEntryDao.getByRemoteId(remoteId) ?: return 0
        return setLogEntryDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = setLogEntryDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return setLogEntryDao.deleteMany(toDelete)
    }
}