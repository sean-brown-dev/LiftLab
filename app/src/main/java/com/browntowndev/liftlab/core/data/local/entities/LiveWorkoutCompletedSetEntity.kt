package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions
import com.browntowndev.liftlab.core.domain.enums.SetType

@GenerateFirestoreMetadataExtensions
@Entity("liveWorkoutCompletedSets",
    indices = [
        Index("liftId"),
        Index("workoutId"),
        Index("synced"),
        Index("remoteId", unique = true),
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
data class LiveWorkoutCompletedSetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("live_workout_completed_set_id")
    val id: Long = 0,
    val workoutId: Long,
    val liftId: Long,
    val setType: SetType,
    val liftPosition: Int,
    val setPosition: Int,
    val myoRepSetPosition: Int? = null,
    val weight: Float,
    val reps: Int,
    val rpe: Float,
    val oneRepMax: Int,
    val missedLpGoals: Int? = null,
    val isDeload: Boolean = false,
): BaseEntity()
