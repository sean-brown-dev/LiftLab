package com.browntowndev.liftlab.core.data.remote.dto

import java.util.Date

data class SyncMetadataDto(
    val collectionName: String,
    val lastSyncTimestamp: Date,
)
