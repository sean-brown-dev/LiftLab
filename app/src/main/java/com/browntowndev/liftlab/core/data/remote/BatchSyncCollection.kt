package com.browntowndev.liftlab.core.data.remote

import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto

data class BatchSyncCollection(
    val collectionName: String,
    val remoteEntities: List<BaseRemoteDto>,
    val syncType: SyncType,
)