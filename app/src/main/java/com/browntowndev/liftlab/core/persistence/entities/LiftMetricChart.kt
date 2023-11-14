package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.LiftMetricChartType

@Entity(
    tableName = "liftMetricCharts",
    indices = [Index("liftId")],
    foreignKeys = [ForeignKey(entity = Lift::class,
        parentColumns = arrayOf("lift_id"),
        childColumns = arrayOf("liftId"),
        onDelete = ForeignKey.CASCADE)]
)
data class LiftMetricChart(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("lift_metric_chart_id")
    val id: Long = 0,
    val liftId: Long? = null,
    val chartType: LiftMetricChartType,
)