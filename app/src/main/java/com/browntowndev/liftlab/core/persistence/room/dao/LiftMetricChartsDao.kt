package com.browntowndev.liftlab.core.persistence.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.room.entities.LiftMetricChartEntity

@Dao
interface LiftMetricChartsDao: BaseDao<LiftMetricChartEntity> {
    @Query("SELECT * FROM liftMetricCharts WHERE lift_metric_chart_id = :id")
    suspend fun get(id: Long): LiftMetricChartEntity?

    @Transaction
    @Query("SELECT * FROM liftMetricCharts")
    suspend fun getAll(): List<LiftMetricChartEntity>

    @Query("DELETE FROM liftMetricCharts")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT c.* " +
            "FROM liftMetricCharts c " +
            "INNER JOIN lifts l ON c.liftId = l.lift_id " +
            "WHERE liftId IS NOT NULL " +
            "ORDER BY l.name, c.chartType")
    suspend fun getAllForExistingLifts(): List<LiftMetricChartEntity>

    @Transaction
    @Query("SELECT * FROM liftMetricCharts WHERE lift_metric_chart_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<LiftMetricChartEntity>

    @Query("SELECT * FROM liftMetricCharts WHERE liftId IS NULL")
    suspend fun getAllWithNoLift(): List<LiftMetricChartEntity>

    @Query("DELETE FROM liftMetricCharts WHERE lift_metric_chart_id = :id")
    suspend fun delete(id: Long)
}