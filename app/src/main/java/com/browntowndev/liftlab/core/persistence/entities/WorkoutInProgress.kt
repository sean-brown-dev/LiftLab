package com.browntowndev.liftlab.core.persistence.entities

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
    indices = [Index("workoutId")],
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["workout_id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WorkoutInProgress(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("workout_in_progress_id")
    val id: Long = 0,
    val workoutId: Long,
    val startTime: Date,
) : BaseEntity()
