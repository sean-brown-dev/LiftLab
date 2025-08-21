package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions
import java.util.Date

@GenerateFirestoreMetadataExtensions
@Entity(
    tableName = "workoutsInProgress",
    indices = [
        Index("workoutId"),
        Index("synced"),
        Index("remoteId", unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["workout_id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WorkoutInProgressEntity(
    @PrimaryKey @ColumnInfo("workout_in_progress_id")
    val id: Long = 1,
    val workoutId: Long,
    val startTime: Date,
) : BaseEntity()
