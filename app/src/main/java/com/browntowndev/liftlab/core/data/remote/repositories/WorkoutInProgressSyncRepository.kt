package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.WorkoutInProgressDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutInProgressRemoteDto
import com.browntowndev.liftlab.core.sync.RemoteCollectionNames
import kotlin.reflect.KClass

class WorkoutInProgressSyncRepository(
    private val workoutInProgressDao: WorkoutInProgressDao,
) : BaseRemoteSyncRepository<WorkoutInProgressRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.WORKOUT_IN_PROGRESS_COLLECTION
    override val remoteDtoClass: KClass<WorkoutInProgressRemoteDto> = WorkoutInProgressRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<WorkoutInProgressRemoteDto> =
        workoutInProgressDao.get().let {
            if (it != null) listOf(it.toRemoteDto()) else emptyList()
        }

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
        return workoutInProgressDao.delete()
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        return workoutInProgressDao.delete()
    }
}