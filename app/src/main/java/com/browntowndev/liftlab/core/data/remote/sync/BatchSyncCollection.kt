package com.browntowndev.liftlab.core.data.remote.sync

import com.browntowndev.liftlab.core.data.common.SyncType

data class BatchSyncCollection(
    val collectionName: String,
    val roomEntityIds: List<Long>,
    val syncType: SyncType,
)
