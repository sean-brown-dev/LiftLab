package com.browntowndev.liftlab.core.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("setLogEntries",
    indices = [Index("liftId"), Index("workoutLogEntryId")],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogEntry::class,
            parentColumns = arrayOf("workout_log_entry_id"),
            childColumns = arrayOf("workoutLogEntryId"),
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Lift::class,
            parentColumns = arrayOf("lift_id"),
            childColumns = arrayOf("liftId"),
            onDelete = ForeignKey.RESTRICT
        ),])
data class SetLogEntry(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "set_log_entry_id")
    val id: Long = 0,
    val workoutLogEntryId: Long,
    val liftId: Long,
    val setPosition: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Int
)
