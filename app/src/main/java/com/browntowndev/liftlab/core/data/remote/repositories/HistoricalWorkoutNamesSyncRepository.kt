package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.HistoricalWorkoutNameRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class HistoricalWorkoutNamesSyncRepository(
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao,
) : BaseRemoteSyncRepository<HistoricalWorkoutNameRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.HISTORICAL_WORKOUT_NAMES_COLLECTION

    override val remoteDtoClass: KClass<HistoricalWorkoutNameRemoteDto> = HistoricalWorkoutNameRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<HistoricalWorkoutNameRemoteDto> =
        historicalWorkoutNamesDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

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

    override suspend fun deleteByRemoteId(remoteId: String): Int {
        val toDelete = historicalWorkoutNamesDao.getByRemoteId(remoteId) ?: return 0
        return historicalWorkoutNamesDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = historicalWorkoutNamesDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return historicalWorkoutNamesDao.deleteMany(toDelete)
    }
}