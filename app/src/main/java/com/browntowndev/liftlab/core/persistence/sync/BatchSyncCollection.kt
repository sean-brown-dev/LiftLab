package com.browntowndev.liftlab.core.persistence.sync

import com.browntowndev.liftlab.core.common.enums.SyncType

data class BatchSyncCollection(
    val collectionName: String,
    val roomEntityIds: List<Long>,
    val syncType: SyncType,
)
