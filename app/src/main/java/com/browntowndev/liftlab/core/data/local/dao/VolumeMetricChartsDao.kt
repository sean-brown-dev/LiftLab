package com.browntowndev.liftlab.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.browntowndev.liftlab.core.data.local.entities.VolumeMetricChartEntity

@Dao
interface VolumeMetricChartsDao: BaseDao<VolumeMetricChartEntity> {
    @Query("SELECT * FROM volumeMetricCharts WHERE synced = 0")
    suspend fun getAllUnsynced(): List<VolumeMetricChartEntity>

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

    @Query("UPDATE volumeMetricCharts SET deleted = 1, synced = 0 WHERE lift_volume_chart_id = :id")
    suspend fun softDelete(id: Long): Int

    @Query("UPDATE volumeMetricCharts SET deleted = 1, synced = 0 WHERE lift_volume_chart_id IN (:ids)")
    suspend fun softDeleteMany(ids: List<Long>): Int

    @Query("SELECT * FROM volumeMetricCharts WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): VolumeMetricChartEntity?

    @Query("SELECT * FROM volumeMetricCharts WHERE remoteId IN (:remoteIds)")
    suspend fun getManyByRemoteId(remoteIds: List<String>): List<VolumeMetricChartEntity>
}