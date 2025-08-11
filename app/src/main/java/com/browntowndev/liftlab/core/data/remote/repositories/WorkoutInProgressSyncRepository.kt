package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutInProgressRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class WorkoutInProgressSyncRepository(
    private val workoutInProgressDao: WorkoutInProgressDao,
) : BaseRemoteSyncRepository<WorkoutInProgressRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.WORKOUT_IN_PROGRESS_COLLECTION
    override val remoteDtoClass: KClass<WorkoutInProgressRemoteDto> = WorkoutInProgressRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<WorkoutInProgressRemoteDto> =
        workoutInProgressDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<WorkoutInProgressRemoteDto> =
        workoutInProgressDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<WorkoutInProgressRemoteDto>): List<Long> =
        workoutInProgressDao.upsertMany(entities.fastMap { it.toEntity() })
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
        val toDelete = workoutInProgressDao.getByRemoteId(remoteId) ?: return 0
        return workoutInProgressDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = workoutInProgressDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return workoutInProgressDao.deleteMany(toDelete)
    }
}