package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName="restTimerInProgress",
)
data class RestTimerInProgress(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("rest_timer_in_progress_id")
    val id: Long = 0,
    val timeStartedInMillis: Long,
    val restTime: Long,
)

