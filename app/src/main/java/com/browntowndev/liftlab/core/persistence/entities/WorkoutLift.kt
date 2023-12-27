package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.ProgressionScheme

@Entity("workoutLifts",
    indices = [Index("liftId"), Index("workoutId")],
    foreignKeys = [
        ForeignKey(entity = Workout::class,
            parentColumns = arrayOf("workout_id"),
            childColumns = arrayOf("workoutId"),
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Lift::class,
            parentColumns = arrayOf("lift_id"),
            childColumns = arrayOf("liftId"),
            onDelete = ForeignKey.CASCADE)]
)
data class WorkoutLift(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("workout_lift_id")
    val id: Long = 0,
    val workoutId: Long,
    val liftId: Long,
    val progressionScheme: ProgressionScheme,
    val position: Int,
    val setCount: Int,
    val deloadWeek: Int? = null,
    val rpeTarget: Float? = null,
    val repRangeBottom: Int? = null,
    val repRangeTop: Int? = null,
    val note: String? = null,
)
