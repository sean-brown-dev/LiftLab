package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class WorkoutsSyncRepository(
    private val workoutsDao: WorkoutsDao,
) : BaseRemoteSyncRepository<WorkoutRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.WORKOUTS_COLLECTION
    override val remoteDtoClass: KClass<WorkoutRemoteDto> = WorkoutRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<WorkoutRemoteDto> {
        TODO("Not yet implemented")
    }

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
}