package com.browntowndev.liftlab.core.persistence.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.persistence.room.entities.VolumeMetricChartEntity

@Dao
interface VolumeMetricChartsDao: BaseDao<VolumeMetricChartEntity> {
    @Query("SELECT * FROM volumeMetricCharts WHERE lift_volume_chart_id = :id")
    suspend fun get(id: Long): VolumeMetricChartEntity?

    @Transaction
    @Query("SELECT * FROM volumeMetricCharts WHERE lift_volume_chart_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<VolumeMetricChartEntity>

    @Query("DELETE FROM volumeMetricCharts")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM volumeMetricCharts")
    suspend fun getAll(): List<VolumeMetricChartEntity>
}