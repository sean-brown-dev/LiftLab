package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart

@Dao
interface VolumeMetricChartDao {
    @Upsert
    fun upsert(chart: VolumeMetricChart): Long

    @Upsert
    fun upsertMany(chart: List<VolumeMetricChart>): List<Long>

    @Query("DELETE FROM volumeMetricCharts WHERE lift_volume_chart_id = :id")
    fun delete(id: Long)

    @Query("SELECT * FROM volumeMetricCharts")
    fun getAll(): List<VolumeMetricChart>
}