package com.browntowndev.liftlab.core.data.remote

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.forEachParallel
import com.browntowndev.liftlab.core.data.common.SyncType
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.reflect.KClass

class FirestoreRemoteDataClient(
    private val firestoreClient: FirestoreClient,
    private val collectionTypes: Map<String, KClass<out BaseRemoteDto>>
): RemoteDataClient {

    override suspend fun getAllSince(collectionName: String, lastUpdated: Date): List<BaseRemoteDto> {
        if (!firestoreClient.isUserLoggedIn) return emptyList()
        return firestoreClient
            .userCollection(collectionName)
            .whereGreaterThanOrEqualTo("lastUpdated", lastUpdated)
            .get()
            .await()
            .documents
            .toDtos(collectionName)
    }

    override suspend fun getMany(collectionName: String, ids: List<String>): List<BaseRemoteDto> {
        if (!firestoreClient.isUserLoggedIn) return emptyList()
        return firestoreClient.userCollection(collectionName)
            .whereIn("id", ids)
            .get()
            .await()
            .documents
            .toDtos(collectionName)
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

            batch.commit()
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