package com.browntowndev.liftlab.core.persistence.room.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.models.LiftMetricChart
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.room.entities.LiftMetricChartEntity
import com.browntowndev.liftlab.core.persistence.room.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry


class LiftMetricChartsRepositoryImpl(
    private val liftMetricChartsDao: LiftMetricChartsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
) : LiftMetricChartsRepository {
    override suspend fun deleteAllWithNoLifts() {
        val toDelete = liftMetricChartsDao.getAllWithNoLift()
        if (toDelete.isEmpty()) return

        liftMetricChartsDao.deleteMany(toDelete)
        val toDeleteInFirestore = toDelete.fastMapNotNull {
            it.firestoreId?.let { _ -> it.id }
        }
        if (toDeleteInFirestore.isEmpty()) return
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                roomEntityIds = toDeleteInFirestore,
                SyncType.Delete,
            )
        )
    }

    override suspend fun upsertMany(models: List<LiftMetricChart>): List<Long> {
        val currentCharts = liftMetricChartsDao.getMany(models.fastMap { it.id })
            .associateBy { it.id }

        val charts = models.fastMap { liftMetricChart ->
            val current = currentCharts[liftMetricChart.id]
            LiftMetricChartEntity(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
            ).applyFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        }

        val upsertIds = liftMetricChartsDao.upsertMany(charts)
        val chartsWithUpdatedIds = charts.zip(upsertIds).map { (chart, id) ->
            if (id == -1L) chart else chart.copy(id = id)
        }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                roomEntityIds = chartsWithUpdatedIds.fastMap { it.id },
                SyncType.Upsert,
            )
        )

        return upsertIds
    }

    override suspend fun upsert(model: LiftMetricChart): Long {
        val current = liftMetricChartsDao.get(model.id)
        val toUpsert =
            LiftMetricChartEntity(
                id = model.id,
                liftId = model.liftId,
                chartType = model.chartType,
            ).applyFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )

        val upsertId = liftMetricChartsDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                roomEntityIds = listOf(upsertId),
                SyncType.Upsert,
            )
        )

        return upsertId
    }

    override suspend fun getAll(): List<LiftMetricChart> {
        return liftMetricChartsDao.getAllForExistingLifts().fastMap {
            LiftMetricChart(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }

    override suspend fun getMany(ids: List<Long>): List<LiftMetricChart> {
        return liftMetricChartsDao.getMany(ids).fastMap {
            LiftMetricChart(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }

    override suspend fun getById(id: Long): LiftMetricChart? {
        return liftMetricChartsDao.get(id)?.let {
            LiftMetricChart(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }

    override suspend fun update(model: LiftMetricChart) {
        val entity = LiftMetricChartEntity(
            id = model.id,
            liftId = model.liftId,
            chartType = model.chartType
        )
        liftMetricChartsDao.update(entity)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                roomEntityIds = listOf(entity.id),
                SyncType.Upsert
            )
        )
    }

    override suspend fun updateMany(models: List<LiftMetricChart>) {
        val entities = models.map {
            LiftMetricChartEntity(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType
            )
        }
        liftMetricChartsDao.updateMany(entities)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                roomEntityIds = entities.map { it.id },
                SyncType.Upsert
            )
        )
    }

    override suspend fun insert(model: LiftMetricChart): Long {
        val entity = LiftMetricChartEntity(
            id = model.id,
            liftId = model.liftId,
            chartType = model.chartType
        )
        val newId = liftMetricChartsDao.insert(entity)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                roomEntityIds = listOf(newId),
                SyncType.Upsert
            )
        )
        return newId
    }

    override suspend fun insertMany(models: List<LiftMetricChart>): List<Long> {
        val entities = models.map {
            LiftMetricChartEntity(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType
            )
        }
        val newIds = liftMetricChartsDao.insertMany(entities)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                roomEntityIds = newIds,
                SyncType.Upsert
            )
        )
        return newIds
    }

    override suspend fun delete(model: LiftMetricChart): Int {
        val entity = LiftMetricChartEntity(
            id = model.id,
            liftId = model.liftId,
            chartType = model.chartType
        )
        val count = liftMetricChartsDao.delete(entity)
        if (entity.firestoreId != null && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                    roomEntityIds = listOf(entity.id),
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteMany(models: List<LiftMetricChart>): Int {
        val entities = models.map {
            LiftMetricChartEntity(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType
            )
        }
        val count = liftMetricChartsDao.deleteMany(entities)
        val toDeleteWithFirestoreIds = entities
            .filter { it.firestoreId != null }
            .fastMap { it.id }
        if (toDeleteWithFirestoreIds.isNotEmpty() && count > 0) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                    roomEntityIds = toDeleteWithFirestoreIds,
                    SyncType.Delete
                )
            )
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = liftMetricChartsDao.get(id)
        return if (toDelete != null) {
            val count = liftMetricChartsDao.delete(toDelete)
            if (toDelete.firestoreId != null && count > 0) {
                firestoreSyncManager.enqueueSyncRequest(
                    SyncQueueEntry(
                        collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                        roomEntityIds = listOf(toDelete.id),
                        SyncType.Delete,
                    )
                )
            }
            count
        } else {
            0
        }
    }
}