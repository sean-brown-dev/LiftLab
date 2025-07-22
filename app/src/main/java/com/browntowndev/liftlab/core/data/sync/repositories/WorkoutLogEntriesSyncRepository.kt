package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutLogEntryRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class WorkoutLogEntriesSyncRepository(
    private val workoutLogEntriesDao: WorkoutLogEntryDao,
) : BaseRemoteSyncRepository<WorkoutLogEntryRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.WORKOUT_LOG_ENTRIES_COLLECTION
    override val remoteDtoClass: KClass<WorkoutLogEntryRemoteDto> = WorkoutLogEntryRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<WorkoutLogEntryRemoteDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllUnsyncedTyped(): List<WorkoutLogEntryRemoteDto> =
        workoutLogEntriesDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<WorkoutLogEntryRemoteDto>): List<Long> =
        workoutLogEntriesDao.upsertMany(entities.fastMap { it.toEntity() })
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