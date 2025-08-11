package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutLiftRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLiftsDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class WorkoutLiftsSyncRepository(
    private val workoutLiftsDao: WorkoutLiftsDao,
) : BaseRemoteSyncRepository<WorkoutLiftRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.WORKOUT_LIFTS_COLLECTION
    override val remoteDtoClass: KClass<WorkoutLiftRemoteDto> = WorkoutLiftRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<WorkoutLiftRemoteDto> =
        workoutLiftsDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<WorkoutLiftRemoteDto> =
        workoutLiftsDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<WorkoutLiftRemoteDto>): List<Long> =
        workoutLiftsDao.upsertMany(entities.fastMap { it.toEntity() })
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
        val toDelete = workoutLiftsDao.getByRemoteId(remoteId) ?: return 0
        return workoutLiftsDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = workoutLiftsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return workoutLiftsDao.deleteMany(toDelete)
    }
}