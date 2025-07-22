package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions
import com.browntowndev.liftlab.core.common.enums.VolumeType
import com.browntowndev.liftlab.core.common.enums.VolumeTypeImpact

@GenerateFirestoreMetadataExtensions
@Entity(
    tableName = "volumeMetricCharts",
)
data class VolumeMetricChartEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("lift_volume_chart_id")
    val id: Long = 0,
    val volumeType: VolumeType,
    val volumeTypeImpact: VolumeTypeImpact,
): BaseEntity()