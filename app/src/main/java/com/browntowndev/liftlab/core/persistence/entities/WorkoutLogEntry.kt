package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity("workoutLogEntries",
    indices = [Index("historicalWorkoutNameId")],
    foreignKeys = [
        ForeignKey(
            entity = HistoricalWorkoutName::class,
            parentColumns = arrayOf("historical_workout_name_id"),
            childColumns = arrayOf("historicalWorkoutNameId"),
            onDelete = ForeignKey.RESTRICT
        )])
data class WorkoutLogEntry(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "workout_log_entry_id")
    val id: Long = 0,
    val historicalWorkoutNameId: Long,
    val programWorkoutCount: Int,
    val programDeloadWeek: Int,
    val mesocycle: Int,
    val microcycle: Int,
    val microcyclePosition: Int,
    val date: Date,
    val durationInMillis: Long,
)
