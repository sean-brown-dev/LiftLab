package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository // ASSUMED DOMAIN REPO
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.LiftFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel // ASSUMED MAPPERS
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc // ASSUMED MAPPERS
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class LiftsDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val liftsRepository: LiftsRepository // Depends on the DOMAIN repository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.LIFTS_COLLECTION
    override val firestoreDocClass: KClass<LiftFirestoreDoc> = LiftFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<LiftFirestoreDoc> {
        return liftsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<LiftFirestoreDoc> {
        return liftsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<LiftFirestoreDoc>> {
        return liftsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }
    
    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<LiftFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            liftsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<LiftFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            liftsRepository.updateMany(models)
        }
    }
    
    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        // Lifts have no parents, so this is a no-op.
        return emptyList()
    }
}