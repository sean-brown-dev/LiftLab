package com.browntowndev.liftlab.core.data.local.views

import androidx.room.ColumnInfo
import androidx.room.DatabaseView
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import kotlin.time.Duration

@DatabaseView("SELECT * FROM lifts WHERE deleted = 0")
data class LiveLiftView(
    @ColumnInfo("lift_id")
    val id: Long,
    val name: String,
    val movementPattern: MovementPattern,
    val volumeTypesBitmask: Int,
    val secondaryVolumeTypesBitmask: Int?,
    val restTime: Duration?,
    val restTimerEnabled: Boolean,
    val incrementOverride: Float?,
    val isBodyweight: Boolean,
    val note: String?,
)
