package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class WorkoutsSyncRepository(
    private val workoutsDao: WorkoutsDao,
) : BaseRemoteSyncRepository<WorkoutRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.WORKOUTS_COLLECTION
    override val remoteDtoClass: KClass<WorkoutRemoteDto> = WorkoutRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<WorkoutRemoteDto> =
        workoutsDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<WorkoutRemoteDto> =
        workoutsDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<WorkoutRemoteDto>): List<Long> =
        workoutsDao.upsertMany(entities.fastMap { it.toEntity() })
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
        val toDelete = workoutsDao.getByRemoteId(remoteId) ?: return 0
        return workoutsDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = workoutsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return workoutsDao.deleteMany(toDelete)
    }
}