package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("historicalWorkoutNames",
    indices = [Index(value = ["programName", "workoutName"])])
data class HistoricalWorkoutName(
    @PrimaryKey @ColumnInfo("historical_workout_name_id")
    val id: Long = 0,
    val programId: Long,
    val workoutId: Long,
    val programName: String,
    val workoutName: String,
)