package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.LiftMetricChartRemoteDto
import com.browntowndev.liftlab.core.sync.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class LiftMetricChartsSyncRepository(
    private val liftMetricChartsDao: LiftMetricChartsDao,
) : BaseRemoteSyncRepository<LiftMetricChartRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.LIFT_METRIC_CHARTS_COLLECTION
    override val remoteDtoClass: KClass<LiftMetricChartRemoteDto> = LiftMetricChartRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<LiftMetricChartRemoteDto> =
        liftMetricChartsDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

    override suspend fun getAllUnsyncedTyped(): List<LiftMetricChartRemoteDto> =
        liftMetricChartsDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<LiftMetricChartRemoteDto>): List<Long> =
        liftMetricChartsDao.upsertMany(entities.fastMap { it.toEntity() })
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
        val toDelete = liftMetricChartsDao.getByRemoteId(remoteId) ?: return 0
        return liftMetricChartsDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = liftMetricChartsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return liftMetricChartsDao.deleteMany(toDelete)
    }
}