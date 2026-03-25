package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import kotlin.time.Duration

@GenerateFirestoreMetadataExtensions
@Entity(tableName = "lifts",
    indices = [
        Index("movementPattern"),
        Index("lift_id", "restTime"),
        Index("synced"),
        Index("remoteId", unique = true),
    ])
data class LiftEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("lift_id")
    val id: Long = 0,
    val name: String,
    val movementPattern: MovementPattern,
    val volumeTypesBitmask: Int,
    val secondaryVolumeTypesBitmask: Int? = null,
    val restTime: Duration? = null,
    @ColumnInfo(defaultValue = true.toString())
    val restTimerEnabled: Boolean = true,
    val incrementOverride: Float? = null,
    @ColumnInfo(defaultValue = false.toString())
    val isBodyweight: Boolean = false,
    val note: String? = null,
): BaseEntity()