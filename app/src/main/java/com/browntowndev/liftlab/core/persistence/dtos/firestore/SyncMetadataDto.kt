package com.browntowndev.liftlab.core.persistence.dtos.firestore

import java.util.Date

data class SyncMetadataDto(
    val collectionName: String,
    val lastSyncTimestamp: Date,
)
