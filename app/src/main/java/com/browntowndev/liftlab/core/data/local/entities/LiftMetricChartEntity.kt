package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions
import com.browntowndev.liftlab.core.domain.enums.LiftMetricChartType

@GenerateFirestoreMetadataExtensions
@Entity(
    tableName = "liftMetricCharts",
    indices = [
        Index("liftId"),
        Index("synced"),
        Index("remoteId", unique = true),
    ],
    foreignKeys = [ForeignKey(entity = LiftEntity::class,
        parentColumns = arrayOf("lift_id"),
        childColumns = arrayOf("liftId"),
        onDelete = ForeignKey.CASCADE)]
)
data class LiftMetricChartEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("lift_metric_chart_id")
    val id: Long = 0,
    val liftId: Long? = null,
    val chartType: LiftMetricChartType,
): BaseEntity()