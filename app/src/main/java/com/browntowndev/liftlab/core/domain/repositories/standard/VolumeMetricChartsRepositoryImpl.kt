package com.browntowndev.liftlab.core.domain.repositories.standard

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.room.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.domain.models.VolumeMetricChart
import com.browntowndev.liftlab.core.persistence.entities.room.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry

class VolumeMetricChartsRepositoryImpl(
    private val volumeMetricChartsDao: VolumeMetricChartsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
) : VolumeMetricChartsRepository {
    override suspend fun upsert(volumeMetricChart: VolumeMetricChart): Long {
        val current = volumeMetricChartsDao.get(volumeMetricChart.id)
        val toUpsert = VolumeMetricChartEntity(
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

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )

        return id
    }

    override suspend fun upsertMany(volumeMetricCharts: List<VolumeMetricChart>): List<Long> {
        val currentEntities = volumeMetricChartsDao.getMany(volumeMetricCharts.map { it.id }).associateBy { it.id }
        var toUpsert =
            volumeMetricCharts.fastMap { volumeMetricChart ->
                val current = currentEntities[volumeMetricChart.id]
                VolumeMetricChartEntity(
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

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                roomEntityIds = toUpsert.fastMap { it.id },
                SyncType.Upsert,
            )
        )

        return upsertIds
    }

    override suspend fun getAll(): List<VolumeMetricChart> {
        return volumeMetricChartsDao.getAll().fastMap {
            VolumeMetricChart(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = volumeMetricChartsDao.get(id) ?: return 0
        volumeMetricChartsDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }
        return 1
    }
}