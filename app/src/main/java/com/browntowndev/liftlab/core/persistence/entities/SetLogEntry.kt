package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.SetType

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
    val setType: SetType? = null,
    val liftPosition: Int,
    val setPosition: Int,
    val myoRepSetPosition: Int? = null,
    val weight: Float,
    val reps: Int,
    val rpe: Float,
    val mesoCycle: Int,
    val microCycle: Int,
)
