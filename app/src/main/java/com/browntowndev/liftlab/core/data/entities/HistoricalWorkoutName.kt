package com.browntowndev.liftlab.core.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("historicalWorkoutNames",
    indices = [Index(value = ["programName", "workoutName"], unique = true)])
data class HistoricalWorkoutName(
    @PrimaryKey @ColumnInfo("workout_name_id")
    val id: String,
    val programName: String,
    val workoutName: String
)