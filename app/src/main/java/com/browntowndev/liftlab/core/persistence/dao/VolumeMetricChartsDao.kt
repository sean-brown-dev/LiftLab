package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart
import kotlinx.coroutines.flow.Flow

@Dao
interface VolumeMetricChartsDao: BaseDao<VolumeMetricChart> {
    @Query("DELETE FROM volumeMetricCharts")
    suspend fun deleteAll()

    @Query("DELETE FROM volumeMetricCharts WHERE lift_volume_chart_id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM volumeMetricCharts")
    suspend fun getAll(): List<VolumeMetricChart>
}