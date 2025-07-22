package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.entities.LiftMetricChartEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiftMetricChartsDao: BaseDao<LiftMetricChartEntity> {
    @Query("SELECT * FROM liftMetricCharts WHERE synced = 0")
    suspend fun getAllUnsynced(): List<LiftMetricChartEntity>

    @Query("SELECT * FROM liftMetricCharts WHERE lift_metric_chart_id = :id AND deleted = 0")
    suspend fun get(id: Long): LiftMetricChartEntity?

    @Transaction
    @Query("SELECT * FROM liftMetricCharts WHERE deleted = 0")
    suspend fun getAll(): List<LiftMetricChartEntity>

    @Transaction
    @Query("SELECT * FROM liftMetricCharts WHERE deleted = 0")
    fun getAllFlow(): Flow<List<LiftMetricChartEntity>>

    @Query("DELETE FROM liftMetricCharts")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT c.* " +
            "FROM liftMetricCharts c " +
            "INNER JOIN lifts l ON c.liftId = l.lift_id " +
            "WHERE liftId IS NOT NULL AND c.deleted = 0" +
            "ORDER BY l.name, c.chartType")
    suspend fun getAllForExistingLifts(): List<LiftMetricChartEntity>

    @Transaction
    @Query("SELECT * FROM liftMetricCharts WHERE lift_metric_chart_id IN (:ids) AND deleted = 0")
    suspend fun getMany(ids: List<Long>): List<LiftMetricChartEntity>

    @Query("SELECT * FROM liftMetricCharts WHERE liftId IS NULL AND deleted = 0")
    suspend fun getAllWithNoLift(): List<LiftMetricChartEntity>

    @Query("DELETE FROM liftMetricCharts WHERE lift_metric_chart_id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE liftMetricCharts SET deleted = 1, synced = 0 WHERE lift_metric_chart_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE liftMetricCharts SET deleted = 1, synced = 0 WHERE lift_metric_chart_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM liftMetricCharts WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): LiftMetricChartEntity?

    @Query("SELECT * FROM liftMetricCharts WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<LiftMetricChartEntity>
}