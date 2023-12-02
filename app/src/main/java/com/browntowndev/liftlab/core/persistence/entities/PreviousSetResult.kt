package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.core.common.enums.SetType

@Entity("previousSetResults",
    indices = [
        Index("workoutId"), Index("liftId"),
        Index("workoutId", "liftId", "setPosition"),
        Index("workoutId", "liftId", "setPosition", "myoRepSetPosition"),
    ],
    foreignKeys = [
        ForeignKey(entity = Workout::class,
            parentColumns = arrayOf("workout_id"),
            childColumns = arrayOf("workoutId"),
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Lift::class,
            parentColumns = arrayOf("lift_id"),
            childColumns = arrayOf("liftId"),
            onDelete = ForeignKey.CASCADE)])
data class PreviousSetResult(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("previously_completed_set_id")
    val id: Long = 0,
    val workoutId: Long,
    val liftId: Long,
    val setType: SetType,
    val liftPosition: Int,
    val setPosition: Int,
    val myoRepSetPosition: Int? = null,
    val weightRecommendation: Float?,
    val weight: Float,
    val reps: Int,
    val rpe: Float,
    val mesoCycle: Int,
    val microCycle: Int,
    val missedLpGoals: Int? = null,
    @ColumnInfo(defaultValue = false.toString())
    val isDeload: Boolean = false,
)
