package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.SetLogEntryRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class SetLogEntriesSyncRepository(
    private val setLogEntryDao: SetLogEntryDao,
) : BaseRemoteSyncRepository<SetLogEntryRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.SET_LOG_ENTRIES_COLLECTION
    override val remoteDtoClass: KClass<SetLogEntryRemoteDto> = SetLogEntryRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<SetLogEntryRemoteDto> {
        TODO("Not yet implemented")
    }

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
}