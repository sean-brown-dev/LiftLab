package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="restTimerInProgress")
data class RestTimerInProgressEntity(
    @PrimaryKey @ColumnInfo("rest_timer_in_progress_id")
    val id: Long = 1,
    val timeStartedInMillis: Long,
    val restTime: Long,
)

