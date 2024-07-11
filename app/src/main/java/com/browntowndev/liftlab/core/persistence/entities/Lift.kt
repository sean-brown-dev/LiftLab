package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import kotlin.time.Duration

@Entity(tableName = "lifts", indices = [Index("movementPattern"), Index("lift_id", "restTime")])
data class Lift(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("lift_id")
    val id: Long = 0,
    val name: String,
    val movementPattern: MovementPattern,
    val volumeTypesBitmask: Int,
    val secondaryVolumeTypesBitmask: Int? = null,
    val restTime: Duration? = null,
    val restTimerEnabled: Boolean = true,
    val incrementOverride: Float? = null,
    val isHidden: Boolean = false,
    val isBodyweight: Boolean = false,
    val note: String? = null,
)