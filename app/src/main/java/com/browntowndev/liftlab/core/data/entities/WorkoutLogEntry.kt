package com.browntowndev.liftlab.core.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity("workoutLogEntries",
    foreignKeys = [
        ForeignKey(
            entity = HistoricalWorkoutName::class,
            parentColumns = arrayOf("workout_name_id"),
            childColumns = arrayOf("workoutNameId"),
            onDelete = ForeignKey.RESTRICT
        )])
data class WorkoutLogEntry(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "workout_log_entry_id") val id: String,
    val historicalWorkoutName: Long,
    val mesocycle: Int,
    val microcycle: Int,
    val date: Date
)
