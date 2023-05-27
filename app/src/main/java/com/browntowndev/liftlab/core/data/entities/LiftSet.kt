package com.browntowndev.liftlab.core.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("sets",
    indices = [Index("workoutLiftId", unique = true)],
    foreignKeys = [ForeignKey(entity = WorkoutLift::class,
        parentColumns = arrayOf("workout_lift_id"),
        childColumns = arrayOf("workoutLiftId"),
        onDelete = ForeignKey.CASCADE)])
data class LiftSet(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val workoutLiftId: Long,
    val position: Int,
    val repRangeBottom: Int,
    val repRangeTop: Int
)
