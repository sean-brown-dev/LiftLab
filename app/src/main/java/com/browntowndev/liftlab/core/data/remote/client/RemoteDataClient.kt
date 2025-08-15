package com.browntowndev.liftlab.core.data.remote.client

import com.browntowndev.liftlab.core.domain.models.sync.SyncDto
import com.browntowndev.liftlab.core.sync.BatchSyncCollection
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface RemoteDataClient {
    val canSync: Boolean
    fun getAllSinceFlow(collectionName: String, lastUpdated: Date): Flow<List<SyncDto>>
    fun getManyFlow(collectionName: String, ids: List<String>): Flow<List<SyncDto>>
    suspend fun executeBatchSync(batches: List<BatchSyncCollection>): List<String>
}