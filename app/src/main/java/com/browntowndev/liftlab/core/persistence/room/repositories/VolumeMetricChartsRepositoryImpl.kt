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

        return id
    }

    override suspend fun upsertMany(models: List<VolumeMetricChart>): List<Long> {
        val currentEntities = volumeMetricChartsDao.getMany(models.map { it.id }).associateBy { it.id }
        var toUpsert =
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

    override suspend fun insert(model: VolumeMetricChart): Long {
        TODO("Not yet implemented")
    }

    override suspend fun insertMany(models: List<VolumeMetricChart>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(model: VolumeMetricChart): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMany(models: List<VolumeMetricChart>): Int {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override suspend fun getMany(ids: List<Long>): List<VolumeMetricChart> {
        TODO("Not yet implemented")
    }

    override suspend fun update(model: VolumeMetricChart) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMany(models: List<VolumeMetricChart>) {
        TODO("Not yet implemented")
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