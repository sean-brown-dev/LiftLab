package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName="workoutsInProgress",
    indices = [Index("workoutId")]
)
data class WorkoutInProgress(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("workout_in_progress_id")
    val id: Long = 0,
    val workoutId: Long,
    val startTime: Date,
)
