package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact

@Entity(
    tableName = "volumeMetricCharts",
)
data class VolumeMetricChart(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("lift_volume_chart_id")
    val id: Long = 0,
    val volumeType: VolumeType,
    val volumeTypeImpact: VolumeTypeImpact,
)