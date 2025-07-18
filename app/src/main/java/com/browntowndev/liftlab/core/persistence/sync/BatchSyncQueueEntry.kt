package com.browntowndev.liftlab.core.persistence.sync

data class BatchSyncQueueEntry(
    val id: String,
    val batch: List<SyncQueueEntry>,
)
