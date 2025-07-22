package com.browntowndev.liftlab.core.data.sync.datasources

import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import kotlinx.coroutines.flow.Flow

/**
 * An internal abstraction for the persistence layer that provides FirestoreSyncManager
 * with the necessary operations and type information for a specific data collection.
 */
interface SyncableDataSource {
    /** The name of the collection (e.g., "lifts", "programs"). */
    val collectionName: String

    /** Fetches a list of Firestore documents from the local Room database by their primary keys. */
    suspend fun getMany(roomEntityIds: List<Long>): List<BaseRemoteDto>

    /** Fetches all Firestore documents from the local Room database. */
    suspend fun getAll(): List<BaseRemoteDto>

    /** Provides a Flow of all Firestore documents from the local Room database. */
    fun getAllFlow(): Flow<List<BaseRemoteDto>>

    /** Inserts or updates a list of documents in the local Room database. */
    suspend fun upsertMany(entities: List<BaseRemoteDto>)

    /** Updates a list of documents in the local Room database. Used after an upload to Firestore. */
    suspend fun updateMany(entities: List<BaseRemoteDto>)
}