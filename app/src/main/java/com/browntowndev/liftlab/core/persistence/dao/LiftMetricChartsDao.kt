package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart

@Dao
interface LiftMetricChartsDao {
    @Transaction
    @Upsert
    suspend fun upsertMany(charts: List<LiftMetricChart>): List<Long>

    @Upsert
    suspend fun upsert(chart: LiftMetricChart): Long

    @Query("SELECT * FROM liftMetricCharts WHERE liftId IS NOT NULL")
    suspend fun getAll(): List<LiftMetricChart>

    @Query("SELECT * FROM liftMetricCharts WHERE lift_metric_chart_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<LiftMetricChart>

    @Query("DELETE FROM liftMetricCharts WHERE liftId IS NULL")
    suspend fun deleteAllWithNoLift()

    @Query(
        "DELETE FROM liftMetricCharts " +
        "WHERE lift_metric_chart_id IN (" +
        "SELECT lift_metric_chart_id " +
        "FROM liftMetricCharts c " +
        "INNER JOIN lifts l ON l.lift_id = c.liftId " +
        "WHERE l.name = :liftName AND " +
        "c.chartType = :chartType)"
    )
    suspend fun deleteForLift(liftName: String, chartType: LiftMetricChartType)
}