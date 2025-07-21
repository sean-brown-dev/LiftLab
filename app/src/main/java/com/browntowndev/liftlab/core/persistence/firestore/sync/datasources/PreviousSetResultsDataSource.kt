package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.PreviousSetResultFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class PreviousSetResultsDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val previousSetResultsRepository: PreviousSetResultsRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION
    override val firestoreDocClass: KClass<PreviousSetResultFirestoreDoc> = PreviousSetResultFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<PreviousSetResultFirestoreDoc> {
        return previousSetResultsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<PreviousSetResultFirestoreDoc> {
        return previousSetResultsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<PreviousSetResultFirestoreDoc>> {
        return previousSetResultsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<PreviousSetResultFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            previousSetResultsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<PreviousSetResultFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            previousSetResultsRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        return emptyList()
    }
}