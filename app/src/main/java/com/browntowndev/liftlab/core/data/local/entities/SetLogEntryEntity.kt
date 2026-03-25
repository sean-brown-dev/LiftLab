package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions
import com.browntowndev.liftlab.core.domain.enums.MovementPattern
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.enums.SetType

@GenerateFirestoreMetadataExtensions
@Entity("setLogEntries",
    indices = [
        Index("workoutLogEntryId"),
        Index("synced"),
        Index(value = ["remoteId"], unique = true),

        // fast path for flows keyed by workout entry
        Index(value = ["workoutLogEntryId", "deleted"], name = "idx_sle_wle_deleted"),

        // most-recent-per-(liftId, setPosition) lookups with optional deload flag
        Index(value = ["liftId", "setPosition", "deleted", "isDeload"], name = "idx_sle_lift_pos_flags"),

        // PR-style aggregations (MAX(oneRepMax) BY liftId) and per-lift reads
        Index(value = ["liftId", "deleted", "oneRepMax"], name = "idx_sle_lift_deleted_orm"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogEntryEntity::class,
            parentColumns = arrayOf("workout_log_entry_id"),
            childColumns = arrayOf("workoutLogEntryId"),
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = LiftEntity::class,
            parentColumns = arrayOf("lift_id"),
            childColumns = arrayOf("liftId"),
            onDelete = ForeignKey.RESTRICT
        ),])
data class SetLogEntryEntity(
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
    val repRangeTop: Int?,
    val repRangeBottom: Int?,
    val rpeTarget: Float,
    val weightRecommendation: Float?,
    val weight: Float,
    val reps: Int,
    val rpe: Float,
    val oneRepMax: Int,
    val setMatching: Boolean? = null,
    val maxSets: Int? = null,
    val repFloor: Int? = null,
    val dropPercentage: Float? = null,
    val isDeload: Boolean = false,
): BaseEntity()
