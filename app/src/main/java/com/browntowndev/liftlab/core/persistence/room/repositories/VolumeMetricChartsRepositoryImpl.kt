package com.browntowndev.liftlab.core.persistence.room.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.room.dao.VolumeMetricChartsDao
import com.browntowndev.liftlab.core.domain.models.VolumeMetricChart
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import com.browntowndev.liftlab.core.persistence.room.entities.VolumeMetricChartEntity
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry

class VolumeMetricChartsRepositoryImpl(
    private val volumeMetricChartsDao: VolumeMetricChartsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
) : VolumeMetricChartsRepository {
    override suspend fun upsert(model: VolumeMetricChart): Long {
        val current = volumeMetricChartsDao.get(model.id)
        val toUpsert = VolumeMetricChartEntity(
            id = model.id,
            volumeType = model.volumeType,
            volumeTypeImpact = model.volumeTypeImpact,
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

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<VolumeMetricChart>): List<Long> {
        val currentEntities = volumeMetricChartsDao.getMany(models.map { it.id }).associateBy { it.id }
        val toUpsert =
            models.fastMap { volumeMetricChart ->
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
        val entityIds = toUpsert.zip(upsertIds).fastMap { (toUpsert, upsertId) ->
            if (upsertId == -1L) toUpsert else toUpsert.copy(id = upsertId)
        }.fastMap { it.id }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                roomEntityIds = entityIds,
                SyncType.Upsert,
            )
        )

        return entityIds
    }

    override suspend fun insert(model: VolumeMetricChart): Long {
        val toInsert = VolumeMetricChartEntity(
            id = model.id,
            volumeType = model.volumeType,
            volumeTypeImpact = model.volumeTypeImpact,
        )
        val id = volumeMetricChartsDao.insert(toInsert)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert
            )
        )
        return id
    }

    override suspend fun insertMany(models: List<VolumeMetricChart>): List<Long> {
        val toInsert = models.map {
            VolumeMetricChartEntity(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
        val ids = volumeMetricChartsDao.insertMany(toInsert)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                roomEntityIds = ids,
                SyncType.Upsert
            )
        )
        return ids
    }

    override suspend fun delete(model: VolumeMetricChart): Int {
        val toDelete = VolumeMetricChartEntity(
            id = model.id,
            volumeType = model.volumeType,
            volumeTypeImpact = model.volumeTypeImpact,
        )
        val count = volumeMetricChartsDao.delete(toDelete)
        if (count > 0 && toDelete.firestoreId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteMany(models: List<VolumeMetricChart>): Int {
        val toDelete = models.map {
            VolumeMetricChartEntity(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
        val count = volumeMetricChartsDao.deleteMany(toDelete)
        val firestoreIds = toDelete.mapNotNull { it.firestoreId }
        if (firestoreIds.isNotEmpty() && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                    roomEntityIds = toDelete.map { it.id },
                    SyncType.Delete
                )
            )
        }
        return count
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

    override suspend fun getById(id: Long): VolumeMetricChart? {
        return volumeMetricChartsDao.get(id)?.let {
            VolumeMetricChart(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
    }

    override suspend fun getMany(ids: List<Long>): List<VolumeMetricChart> {
        return volumeMetricChartsDao.getMany(ids).map {
            VolumeMetricChart(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
    }

    override suspend fun update(model: VolumeMetricChart) {
        val toUpdate = VolumeMetricChartEntity(
            id = model.id,
            volumeType = model.volumeType,
            volumeTypeImpact = model.volumeTypeImpact,
        )
        volumeMetricChartsDao.update(toUpdate)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                roomEntityIds = listOf(toUpdate.id),
                SyncType.Upsert
            )
        )
    }

    override suspend fun updateMany(models: List<VolumeMetricChart>) {
        val toUpdate = models.map {
            VolumeMetricChartEntity(
                id = it.id,
                volumeType = it.volumeType,
                volumeTypeImpact = it.volumeTypeImpact,
            )
        }
        volumeMetricChartsDao.updateMany(toUpdate)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                roomEntityIds = toUpdate.map { it.id },
                SyncType.Upsert
            )
        )
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = volumeMetricChartsDao.get(id) ?: return 0
        val count = volumeMetricChartsDao.delete(toDelete)
        if (count > 0 && toDelete.firestoreId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }
        return count
    }
}