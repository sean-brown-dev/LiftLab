package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.SetType

@Entity("sets",
    indices = [Index("workoutLiftId"), Index("workoutLiftId", "position")],
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
    val rpeTarget: Float,
    val repRangeBottom: Int,
    val repRangeTop: Int,
    val repFloor: Int? = null,
    val dropPercentage: Float? = null,
    val maxSets: Int? = null,
    val setMatching: Boolean = false,
    val matchSetGoal: Int? = null,
)
