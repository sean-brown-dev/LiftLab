package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart

@Dao
interface LiftMetricChartsDao {
    @Transaction
    @Upsert
    suspend fun upsertMany(charts: List<LiftMetricChart>): List<Long>

    @Upsert
    suspend fun upsert(chart: LiftMetricChart): Long

    @Query("SELECT c.* " +
            "FROM liftMetricCharts c " +
            "INNER JOIN lifts l ON c.liftId = l.lift_id " +
            "WHERE liftId IS NOT NULL " +
            "ORDER BY l.name, c.chartType")
    suspend fun getAll(): List<LiftMetricChart>

    @Query("SELECT * FROM liftMetricCharts WHERE lift_metric_chart_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<LiftMetricChart>

    @Query("DELETE FROM liftMetricCharts WHERE liftId IS NULL")
    suspend fun deleteAllWithNoLift()

    @Query("DELETE FROM liftMetricCharts WHERE lift_metric_chart_id = :id")
    suspend fun delete(id: Long)
}