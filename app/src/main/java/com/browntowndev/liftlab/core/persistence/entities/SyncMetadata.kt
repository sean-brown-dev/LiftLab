package com.browntowndev.liftlab.core.persistence.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "syncMetadata")
data class SyncMetadata(
    @PrimaryKey
    val collectionName: String,
    val lastSyncTimestamp: Date,
)
