package com.browntowndev.liftlab.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "syncMetadata")
data class SyncMetadataEntity(
    @PrimaryKey
    val collectionName: String,
    val lastSyncTimestamp: Date,
)
