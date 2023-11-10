package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.entities.LiftMetricChart

@Dao
interface LiftMetricChartsDao {
    @Upsert
    suspend fun upsertMany(charts: List<LiftMetricChart>): List<Long>

    @Upsert
    suspend fun upsert(chart: LiftMetricChart): Long

    @Query("SELECT * FROM liftMetricCharts WHERE liftId IS NOT NULL")
    suspend fun getAll(): List<LiftMetricChart>

    @Query("DELETE FROM liftMetricCharts WHERE liftId IS NULL")
    suspend fun deleteAllWithNoLift()
}