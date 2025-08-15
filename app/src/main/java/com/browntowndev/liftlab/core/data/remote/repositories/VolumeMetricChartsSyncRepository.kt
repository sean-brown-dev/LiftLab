package com.browntowndev.liftlab.core.data.remote.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.remote.dto.VolumeMetricChartRemoteDto
import com.browntowndev.liftlab.core.sync.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.local.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.mapping.toRemoteDto
import kotlin.collections.map
import kotlin.reflect.KClass

class VolumeMetricChartsSyncRepository(
    private val volumeMetricChartsDao: VolumeMetricChartsDao,
) : BaseRemoteSyncRepository<VolumeMetricChartRemoteDto>() {
    override val collectionName: String = RemoteCollectionNames.VOLUME_METRIC_CHARTS_COLLECTION
    override val remoteDtoClass: KClass<VolumeMetricChartRemoteDto> = VolumeMetricChartRemoteDto::class

    override suspend fun getManyByRemoteIdTyped(remoteIds: List<String>): List<VolumeMetricChartRemoteDto> =
        volumeMetricChartsDao.getManyByRemoteId(remoteIds).map { it.toRemoteDto() }

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

    override suspend fun deleteByRemoteId(remoteId: String): Int {
        val toDelete = volumeMetricChartsDao.getByRemoteId(remoteId) ?: return 0
        return volumeMetricChartsDao.delete(toDelete)
    }

    override suspend fun deleteManyByRemoteId(remoteIds: List<String>): Int {
        val toDelete = volumeMetricChartsDao.getManyByRemoteId(remoteIds)
        if (toDelete.isEmpty()) return 0
        return volumeMetricChartsDao.deleteMany(toDelete)
    }
}