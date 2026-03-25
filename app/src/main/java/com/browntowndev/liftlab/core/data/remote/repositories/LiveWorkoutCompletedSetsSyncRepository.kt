package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.LiveWorkoutCompletedSetsDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.LiveWorkoutCompletedSetDto
import com.browntowndev.liftlab.core.sync.RemoteCollectionNames
import kotlin.reflect.KClass

class LiveWorkoutCompletedSetsSyncRepository(
    private val liveWorkoutCompletedSetsDao: LiveWorkoutCompletedSetsDao
) : BaseRemoteSyncRepository<LiveWorkoutCompletedSetDto>() {
    override val collectionName: String = RemoteCollectionNames.LIVE_WORKOUT_COMPLETED_SETS_COLLECTION
    override val remoteDtoClass: KClass<LiveWorkoutCompletedSetDto> = LiveWorkoutCompletedSetDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<LiveWorkoutCompletedSetDto> =
        liveWorkoutCompletedSetsDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<LiveWorkoutCompletedSetDto> =
        liveWorkoutCompletedSetsDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<LiveWorkoutCompletedSetDto>): List<Long> =
        liveWorkoutCompletedSetsDao.upsertMany(entities.fastMap { it.toEntity() })
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
        val toDelete = liveWorkoutCompletedSetsDao.getByRemoteId(remoteId) ?: return 0
        return liveWorkoutCompletedSetsDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = liveWorkoutCompletedSetsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return liveWorkoutCompletedSetsDao.deleteMany(toDelete)
    }
}