package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.HistoricalWorkoutNameRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class HistoricalWorkoutNamesSyncRepository(
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao,
) : BaseRemoteSyncRepository<HistoricalWorkoutNameRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.HISTORICAL_WORKOUT_NAMES_COLLECTION
    override val remoteDtoClass: KClass<HistoricalWorkoutNameRemoteDto> = HistoricalWorkoutNameRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<HistoricalWorkoutNameRemoteDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllUnsyncedTyped(): List<HistoricalWorkoutNameRemoteDto> =
        historicalWorkoutNamesDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<HistoricalWorkoutNameRemoteDto>): List<Long> =
        historicalWorkoutNamesDao.upsertMany(entities.fastMap { it.toEntity() })
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