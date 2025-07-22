package com.browntowndev.liftlab.core.data.sync

import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import java.util.Date

interface RemoteDataClient {
    suspend fun getAllSince(collectionName: String, lastUpdated: Date): List<BaseRemoteDto>
    suspend fun getMany(collectionName: String, ids: List<String>): List<BaseRemoteDto>
    suspend fun executeBatchSync(batches: List<BatchSyncCollection>)
}