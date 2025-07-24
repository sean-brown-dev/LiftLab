package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions

@GenerateFirestoreMetadataExtensions
@Entity("historicalWorkoutNames",
    indices = [
        Index(value = ["programName", "workoutName"]),
        Index("synced"),
        Index("remoteId", unique = true),
    ])
data class HistoricalWorkoutNameEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("historical_workout_name_id")
    val id: Long = 0,
    val programId: Long,
    val workoutId: Long,
    val programName: String,
    val workoutName: String,
): BaseEntity()