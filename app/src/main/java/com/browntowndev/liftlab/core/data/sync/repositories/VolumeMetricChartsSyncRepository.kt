package com.browntowndev.liftlab.core.data.sync.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.VolumeMetricChartRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.RemoteMappingExtensions.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class VolumeMetricChartsSyncRepository(
    private val volumeMetricChartsDao: VolumeMetricChartsDao,
) : BaseRemoteSyncRepository<VolumeMetricChartRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.VOLUME_METRIC_CHARTS_COLLECTION
    override val remoteDtoClass: KClass<VolumeMetricChartRemoteDto> = VolumeMetricChartRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<VolumeMetricChartRemoteDto> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllUnsyncedTyped(): List<VolumeMetricChartRemoteDto> =
        volumeMetricChartsDao.getAllUnsynced().map { it.toRemoteDto() }

    override suspend fun upsertManyTyped(entities: List<VolumeMetricChartRemoteDto>): List<Long> =
        volumeMetricChartsDao.upsertMany(entities.fastMap { it.toEntity() })
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