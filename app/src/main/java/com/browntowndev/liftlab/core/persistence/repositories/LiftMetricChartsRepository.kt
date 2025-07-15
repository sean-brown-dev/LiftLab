package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.LiftMetricChartsDao
import com.browntowndev.liftlab.core.persistence.dtos.LiftMetricChartDto
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager


class LiftMetricChartsRepository(
    private val liftMetricChartsDao: LiftMetricChartsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {
    suspend fun deleteAllWithNoLifts() {
        val toDelete = liftMetricChartsDao.getAllWithNoLift()
        liftMetricChartsDao.deleteMany(toDelete)
        firestoreSyncManager.deleteMany(
            collectionName = FirebaseConstants.LIFT_METRIC_CHARTS_COLLECTION,
            firestoreIds = toDelete.mapNotNull { it.firestoreId },
        )
    }

    suspend fun delete(id: Long) {
        liftMetricChartsDao.get(id)?.let { toDelete ->
            liftMetricChartsDao.delete(toDelete)

            if (toDelete.firestoreId != null) {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirebaseConstants.LIFT_METRIC_CHARTS_COLLECTION,
                    firestoreId = toDelete.firestoreId!!,
                )
            }
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
            ).copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        }

        val insertedIds = liftMetricChartsDao.upsertMany(charts)
        firestoreSyncManager.syncMany(
            collectionName = FirebaseConstants.LIFT_METRIC_CHARTS_COLLECTION,
            entities = charts.fastMap { it.toFirebaseDto() },
            onSynced = { firebaseEntities ->
                liftMetricChartsDao.updateMany(firebaseEntities.fastMap { it.toEntity() })
            }
        )

        return insertedIds
    }

    suspend fun upsert(liftMetricChart: LiftMetricChartDto): Long {
        val current = liftMetricChartsDao.get(liftMetricChart.id)
        val toUpsert =
            LiftMetricChart(
                id = liftMetricChart.id,
                liftId = liftMetricChart.liftId,
                chartType = liftMetricChart.chartType,
            ).copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )

        val insertId = liftMetricChartsDao.upsert(toUpsert)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.LIFT_METRIC_CHARTS_COLLECTION,
            entity = toUpsert.toFirebaseDto(),
            onSynced = { firebaseEntity ->
                liftMetricChartsDao.update(firebaseEntity.toEntity())
            }
        )

        return insertId
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