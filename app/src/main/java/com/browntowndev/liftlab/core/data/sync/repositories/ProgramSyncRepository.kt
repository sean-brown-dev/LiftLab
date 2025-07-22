package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.ProgramRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class ProgramSyncRepository(
    private val programsDao: ProgramsDao,
) : BaseRemoteSyncRepository<ProgramRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.PROGRAMS_COLLECTION
    override val remoteDtoClass: KClass<ProgramRemoteDto> = ProgramRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<ProgramRemoteDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllUnsyncedTyped(): List<ProgramRemoteDto> =
        programsDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<ProgramRemoteDto>): List<Long> =
        programsDao.upsertMany(entities.fastMap { it.toEntity() })
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