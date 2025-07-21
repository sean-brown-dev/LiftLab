package com.browntowndev.liftlab.core.persistence.firestore.sync

import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import java.util.Date
import kotlin.reflect.KClass

/**
 * An internal abstraction for the persistence layer that provides FirestoreSyncManager
 * with the necessary operations and type information for a specific data collection.
 */
interface SyncableDataSource {
    /** The name of the Firestore collection (e.g., "lifts", "programs"). */
    val collectionName: String

    /** The KClass of the Firestore document model (e.g., LiftFirestoreDoc::class). This is crucial for deserialization. */
    val firestoreDocClass: KClass<out BaseFirestoreDoc>

    /** A reference to the specific user's collection in Firestore. */
    val collection: CollectionReference

    /** Fetches a list of Firestore documents from the local Room database by their primary keys. */
    suspend fun getMany(roomEntityIds: List<Long>): List<BaseFirestoreDoc>

    /** Fetches all Firestore documents from the local Room database. */
    suspend fun getAll(): List<BaseFirestoreDoc>

    /** Provides a Flow of all Firestore documents from the local Room database. */
    fun getAllFlow(): Flow<List<BaseFirestoreDoc>>

    /**
     * For a given list of entities, determines the firestoreIds of any parent entities
     * that have been deleted locally. This is used by the deletion watcher.
     * Return an empty list if there are no parent dependencies.
     */
    suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String>
    
    /** Inserts or updates a list of documents in the local Room database. */
    suspend fun upsertMany(entities: List<BaseFirestoreDoc>)

    /** Updates a list of documents in the local Room database. Used after an upload to Firestore. */
    suspend fun updateMany(entities: List<BaseFirestoreDoc>)
}