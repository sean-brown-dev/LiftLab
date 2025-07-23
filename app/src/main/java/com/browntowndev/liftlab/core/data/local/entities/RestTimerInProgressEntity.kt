package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.browntowndev.liftlab.annotations.GenerateFirestoreMetadataExtensions

@Entity(tableName="restTimerInProgress")
data class RestTimerInProgressEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo("rest_timer_in_progress_id")
    val id: Long = 0,
    val timeStartedInMillis: Long,
    val restTime: Long,
)

