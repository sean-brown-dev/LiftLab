package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.LiftMetricChartDto
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.sync.SyncQueueEntry
import kotlinx.coroutines.CoroutineScope


class LiftMetricChartsRepository(
    private val liftMetricChartsDao: LiftMetricChartsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {
    suspend fun deleteAllWithNoLifts() {
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

    suspend fun delete(id: Long) {
        val toDelete = liftMetricChartsDao.get(id) ?: return
        liftMetricChartsDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }
    }

    suspend fun upsertMany(liftMetricCharts: List<LiftMetricChartDto>): List<Long> {
        val currentCharts = liftMetricChartsDao.getMany(liftMetricCharts.fastMap { it.id })
            .associateBy { it.id }

        val charts = liftMetricCharts.fastMap { liftMetricChart ->
            val current = currentCharts[liftMetricChart.id]
            LiftMetricChart(
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

    suspend fun upsert(liftMetricChart: LiftMetricChartDto): Long {
        val current = liftMetricChartsDao.get(liftMetricChart.id)
        val toUpsert =
            LiftMetricChart(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
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

    suspend fun getAll(): List<LiftMetricChartDto> {
        return liftMetricChartsDao.getAllForExistingLifts().fastMap {
            LiftMetricChartDto(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }

    suspend fun getMany(ids: List<Long>): List<LiftMetricChartDto> {
        return liftMetricChartsDao.getMany(ids).fastMap {
            LiftMetricChartDto(
                id = it.id,
                liftId = it.liftId,
                chartType = it.chartType,
            )
        }
    }
}