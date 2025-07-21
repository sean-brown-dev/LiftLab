package com.browntowndev.liftlab.core.persistence.firestore.entities

import java.util.Date

data class SyncMetadataDto(
    val collectionName: String,
    val lastSyncTimestamp: Date,
)
