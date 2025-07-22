package com.browntowndev.liftlab.core.data.remote.sync

data class BatchSyncQueueEntry(
    val id: String,
    val batch: List<SyncQueueEntry>,
)
