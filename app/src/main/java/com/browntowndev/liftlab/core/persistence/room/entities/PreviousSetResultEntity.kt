package com.browntowndev.liftlab.core.persistence.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions
import com.browntowndev.liftlab.core.common.enums.SetType

@GenerateFirestoreMetadataExtensions
@Entity("previousSetResults",
    indices = [
        Index("workoutId"), Index("liftId"),
        Index("workoutId", "liftId", "setPosition"),
        Index("workoutId", "liftId", "setPosition", "myoRepSetPosition"),
    ],
    foreignKeys = [
        ForeignKey(entity = WorkoutEntity::class,
            parentColumns = arrayOf("workout_id"),
            childColumns = arrayOf("workoutId"),
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = LiftEntity::class,
            parentColumns = arrayOf("lift_id"),
            childColumns = arrayOf("liftId"),
            onDelete = ForeignKey.CASCADE)])
data class PreviousSetResultEntity(
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
    @ColumnInfo(defaultValue = 0.toString())
    val oneRepMax: Int,
    val mesoCycle: Int,
    val microCycle: Int,
    val missedLpGoals: Int? = null,
    @ColumnInfo(defaultValue = false.toString())
    val isDeload: Boolean = false,
): BaseEntity()
