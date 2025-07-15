package com.browntowndev.liftlab.core.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.browntowndev.liftlab.core.persistence.entities.VolumeMetricChart
import kotlinx.coroutines.flow.Flow

@Dao
interface VolumeMetricChartsDao: BaseDao<VolumeMetricChart> {
    @Query("SELECT * FROM volumeMetricCharts WHERE lift_volume_chart_id = :id")
    suspend fun get(id: Long): VolumeMetricChart?

    @Transaction
    @Query("SELECT * FROM volumeMetricCharts WHERE lift_volume_chart_id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<VolumeMetricChart>

    @Query("DELETE FROM volumeMetricCharts")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM volumeMetricCharts")
    suspend fun getAll(): List<VolumeMetricChart>
}