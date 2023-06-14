package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("previouslyCompletedSets",
    indices = [Index("workoutId"), Index("liftId"), Index("workoutId", "liftId", "setPosition")],
    foreignKeys = [
        ForeignKey(entity = Workout::class,
            parentColumns = arrayOf("workout_id"),
            childColumns = arrayOf("workoutId"),
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Lift::class,
            parentColumns = arrayOf("lift_id"),
            childColumns = arrayOf("liftId"),
            onDelete = ForeignKey.CASCADE)])
data class PreviouslyCompletedSet(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("previously_completed_set_id")
    val id: Long = 0,
    val workoutId: Long,
    val liftId: Long,
    val setPosition: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Int
)
