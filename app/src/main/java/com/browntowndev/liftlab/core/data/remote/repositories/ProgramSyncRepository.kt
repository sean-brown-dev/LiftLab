package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.ProgramRemoteDto
import com.browntowndev.liftlab.core.sync.RemoteCollectionNames
import kotlin.reflect.KClass

class ProgramSyncRepository(
    private val programsDao: ProgramsDao,
) : BaseRemoteSyncRepository<ProgramRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.PROGRAMS_COLLECTION
    override val remoteDtoClass: KClass<ProgramRemoteDto> = ProgramRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<ProgramRemoteDto> =
        programsDao.getManyByRemoteId(remoteIds).map { it.programEntity.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<ProgramRemoteDto> =
        programsDao.getAllUnsynced().map { it.programEntity.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<ProgramRemoteDto>): List<Long> {
        return programsDao.upsertMany(entities.fastMap { it.toEntity() })
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

    override suspend fun deleteByRemoteId(remoteId: String): Int {
        val toDelete = programsDao.getByRemoteId(remoteId) ?: return 0
        return programsDao.delete(toDelete.programEntity)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = programsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return programsDao.deleteMany(toDelete.fastMap { it.programEntity })
    }
}