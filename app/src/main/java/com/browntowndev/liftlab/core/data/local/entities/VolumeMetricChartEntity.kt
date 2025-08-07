package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions
import com.browntowndev.liftlab.core.domain.enums.VolumeType
import com.browntowndev.liftlab.core.domain.enums.VolumeTypeImpactSelection

@GenerateFirestoreMetadataExtensions
@Entity(
    tableName = "volumeMetricCharts",
    indices = [
        Index("synced"),
        Index("remoteId", unique = true),
    ]
)
data class VolumeMetricChartEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("lift_volume_chart_id")
    val id: Long = 0,
    val volumeType: VolumeType,
    val volumeTypeImpactSelection: VolumeTypeImpactSelection,
): BaseEntity()