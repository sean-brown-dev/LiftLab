package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.MovementPattern
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme
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
    val workoutLiftDeloadWeek: Int? = null,
    val liftName: String,
    val liftMovementPattern: MovementPattern,
    val progressionScheme: ProgressionScheme,
    val setType: SetType,
    val liftPosition: Int,
    val setPosition: Int,
    val myoRepSetPosition: Int? = null,
    val repRangeTop: Int,
    val repRangeBottom: Int,
    val rpeTarget: Float,
    val weightRecommendation: Float?,
    val weight: Float,
    val reps: Int,
    val rpe: Float,
    val mesoCycle: Int,
    val microCycle: Int,
    val setMatching: Boolean? = null,
    val maxSets: Int? = null,
    val repFloor: Int? = null,
    val dropPercentage: Float? = null,
)
