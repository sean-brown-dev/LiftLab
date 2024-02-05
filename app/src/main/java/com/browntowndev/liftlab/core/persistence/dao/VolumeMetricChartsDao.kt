package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart

@Dao
interface VolumeMetricChartsDao {
    @Upsert
    suspend fun upsert(chart: VolumeMetricChart): Long

    @Upsert
    suspend fun upsertMany(chart: List<VolumeMetricChart>): List<Long>

    @Query("DELETE FROM volumeMetricCharts WHERE lift_volume_chart_id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM volumeMetricCharts")
    suspend fun getAll(): List<VolumeMetricChart>
}