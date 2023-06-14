package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.MovementPattern

@Entity(tableName = "lifts", indices = [Index("movementPattern")])
data class Lift(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("lift_id")
    val id: Long = 0,
    val name: String,
    val movementPattern: MovementPattern,
    val volumeTypesBitmask: Int,
    val incrementOverride: Double?,
    val isHidden: Boolean = false,
    val isBodyweight: Boolean = false
)