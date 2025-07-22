package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.LiftMetricChartRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class LiftMetricChartsSyncRepository(
    private val liftMetricChartsDao: LiftMetricChartsDao,
) : BaseRemoteSyncRepository<LiftMetricChartRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.LIFT_METRIC_CHARTS_COLLECTION
    override val remoteDtoClass: KClass<LiftMetricChartRemoteDto> = LiftMetricChartRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<LiftMetricChartRemoteDto> {
        TODO("Not yet implemented")
    }

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
}