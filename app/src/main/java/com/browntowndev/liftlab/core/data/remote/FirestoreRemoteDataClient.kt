package com.browntowndev.liftlab.core.data.remote

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.forEachParallel
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.reflect.KClass

class FirestoreRemoteDataClient(
    private val firestoreClient: FirestoreClient,
    private val collectionTypes: Map<String, KClass<out BaseRemoteDto>>
): RemoteDataClient {

    companion object {
        private const val BATCH_SIZE = 400L
    }

    override val canSync: Boolean get() = firestoreClient.isUserLoggedIn

    /**
     * Returns a Flow that emits batches of documents updated since a given date.
     */
    override fun getAllSinceFlow(collectionName: String, lastUpdated: Date): Flow<List<BaseRemoteDto>> =
        flow {
            if (!firestoreClient.isUserLoggedIn) return@flow

            emitAll(
                flow = firestoreClient.userCollection(collectionName)
                    .whereGreaterThanOrEqualTo("lastUpdated", lastUpdated)
                    .orderBy("lastUpdated", Query.Direction.ASCENDING)
                    .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
                    .executeBatched(collectionName)
            )
        }

    /**
     * Returns a Flow that emits chunks of documents with the given IDs.
     */
    override fun getManyFlow(collectionName: String, ids: List<String>): Flow<List<BaseRemoteDto>> =
        flow {
            if (!firestoreClient.isUserLoggedIn || ids.isEmpty()) {
                emit(emptyList()) // Emit an empty list and complete the flow
                return@flow
            }

            // Firestore's `whereIn` query is limited to 30 items.
            // We break the list of IDs into chunks to handle lists larger than 30.
            ids.chunked(30).fastForEach { idChunk ->
                emit(
                    firestoreClient.userCollection(collectionName)
                        .whereIn(FieldPath.documentId(), idChunk)
                        .get()
                        .await()
                        .documents
                        .toDtos(collectionName)
                )
            }
        }

    private fun Query.executeBatched(collectionName: String): Flow<List<BaseRemoteDto>> =
        flow {
            if (!firestoreClient.isUserLoggedIn) return@flow

            var query = this@executeBatched.limit(BATCH_SIZE)
            var lastVisibleDocument: DocumentSnapshot? = null

            while (true) {
                // If we have a cursor from the previous batch, apply it.
                // This reassigns `query` to the new query object returned by `startAfter`.
                // Need to do this because queries are immutable
                lastVisibleDocument?.let {
                    query = query.startAfter(it)
                }

                val documents = query.get()
                    .await()
                    .documents

                if (documents.isNotEmpty()) {
                    emit(documents.toDtos(collectionName))
                    lastVisibleDocument = documents.last()
                }

                // If we get fewer documents than we asked for, or none at all, we're at the end.
                if (documents.isEmpty() || documents.size < BATCH_SIZE) {
                    break
                }
            }
        }

    override suspend fun executeBatchSync(batches: List<BatchSyncCollection>): List<String> {
        if (!firestoreClient.isUserLoggedIn) return emptyList()

        val upsertDocumentIds = mutableListOf<String>()
        batches.forEachParallel(10) { syncCollectionChunk ->
            val batch = firestoreClient.batch()

            syncCollectionChunk.fastMap { batchSyncCollection ->
                val collectionRef = firestoreClient.userCollection(batchSyncCollection.collectionName)
                when (batchSyncCollection.syncType) {
                    SyncType.Upsert -> batchSyncCollection.remoteEntities.map { entity ->
                        val document = if (entity.remoteId == null) collectionRef.document()
                            else collectionRef.document(entity.remoteId!!)

                        upsertDocumentIds.add(document.id)
                        batch.set(document, entity)
                    }

                    SyncType.Delete -> batchSyncCollection.remoteEntities
                        .mapNotNull { it.remoteId }
                        .map { id ->
                            batch.delete(collectionRef.document(id))
                        }
                }
            }

            batch.commit().await()
        }

        return upsertDocumentIds
    }

    private fun List<DocumentSnapshot>.toDtos(collectionName: String): List<BaseRemoteDto> {
        val clazz = getClazz(collectionName)
        return this.mapNotNull { document -> document.toObject(clazz) }
    }

    private fun getClazz(collectionName: String): Class<out BaseRemoteDto> {
        return collectionTypes[collectionName]?.java ?: throw IllegalArgumentException("No KClass found for collection: $collectionName")
    }
}