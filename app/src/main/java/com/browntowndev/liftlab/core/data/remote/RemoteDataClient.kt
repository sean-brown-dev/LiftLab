package com.browntowndev.liftlab.core.data.remote

import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface RemoteDataClient {
    val canSync: Boolean
    fun getAllSince(collectionName: String, lastUpdated: Date): Flow<List<BaseRemoteDto>>
    fun getMany(collectionName: String, ids: List<String>): Flow<List<BaseRemoteDto>>
    suspend fun executeBatchSync(batches: List<BatchSyncCollection>): List<String>
}