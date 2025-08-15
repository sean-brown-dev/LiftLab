package com.browntowndev.liftlab.core.sync

import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.domain.models.sync.SyncDto

data class BatchSyncCollection(
    val collectionName: String,
    val remoteEntities: List<SyncDto>,
    val syncType: SyncType,
)