package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.SetType

@Entity("sets",
    indices = [Index("workoutLiftId")],
    foreignKeys = [ForeignKey(entity = WorkoutLift::class,
        parentColumns = arrayOf("workout_lift_id"),
        childColumns = arrayOf("workoutLiftId"),
        onDelete = ForeignKey.CASCADE)])
data class CustomLiftSet(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("set_id")
    val id: Long = 0,
    val workoutLiftId: Long,
    val type: SetType,
    val position: Int,
    val repFloor: Int? = null,
    val rpeTarget: Double? = null,
    val repRangeBottom: Int? = null,
    val repRangeTop: Int? = null,
    val dropPercentage: Double? = null,
    val maxSets: Int? = null,
    val setMatching: Boolean = false,
    val matchSetGoal: Int? = null,
)