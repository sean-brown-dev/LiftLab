package com.browntowndev.liftlab.core.persistence.firestore.sync

data class BatchSyncQueueEntry(
    val id: String,
    val batch: List<SyncQueueEntry>,
)
