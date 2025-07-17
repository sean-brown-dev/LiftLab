package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.VolumeMetricChartDto
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope

class VolumeMetricChartsRepository(
    private val volumeMetricChartsDao: VolumeMetricChartsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val syncScope: CoroutineScope,
) {
    suspend fun upsert(volumeMetricChart: VolumeMetricChartDto): Long {
        val current = volumeMetricChartsDao.get(volumeMetricChart.id)
        val toUpsert = VolumeMetricChart(
            id = volumeMetricChart.id,
            volumeType = volumeMetricChart.volumeType,
            volumeTypeImpact = volumeMetricChart.volumeTypeImpact,
        ).applyFirestoreMetadata(
            firestoreId = current?.firestoreId,
            lastUpdated = current?.lastUpdated,
            synced = false
        )
        val id = volumeMetricChartsDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                entity = toUpsert.toFirestoreDto().copy(id = id),
                onSynced = {
                    volumeMetricChartsDao.update(it.toEntity())
                }
            )
        }

        return id
    }

    suspend fun upsertMany(volumeMetricCharts: List<VolumeMetricChartDto>): List<Long> {
        val currentEntities = volumeMetricChartsDao.getMany(volumeMetricCharts.map { it.id }).associateBy { it.id }
        var toUpsert =
            volumeMetricCharts.fastMap { volumeMetricChart ->
                val current = currentEntities[volumeMetricChart.id]
                VolumeMetricChart(
                    id = volumeMetricChart.id,
                    volumeType = volumeMetricChart.volumeType,
                    volumeTypeImpact = volumeMetricChart.volumeTypeImpact,
                ).applyFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false
                )
            }
        val upsertIds = volumeMetricChartsDao.upsertMany(toUpsert)

        toUpsert = toUpsert.zip(upsertIds).fastMap { (toUpsert, upsertId) ->
            if (upsertId == -1L) toUpsert else toUpsert.copy(id = upsertId)
        }

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncMany(
                collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                entities = toUpsert.map { it.toFirestoreDto() },
                onSynced = { firestoreEntities ->
                    volumeMetricChartsDao.updateMany(firestoreEntities.fastMap { it.toEntity() })
                }
            )
        }

        return upsertIds
    }

    suspend fun getAll(): List<VolumeMetricChartDto> {
        return volumeMetricChartsDao.getAll().fastMap {
            VolumeMetricChartDto(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
    }

    suspend fun delete(id: Long) {
        val toDelete = volumeMetricChartsDao.get(id) ?: return
        volumeMetricChartsDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            syncScope.fireAndForgetSync {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                    firestoreId = toDelete.firestoreId!!
                )
            }
        }
    }
}