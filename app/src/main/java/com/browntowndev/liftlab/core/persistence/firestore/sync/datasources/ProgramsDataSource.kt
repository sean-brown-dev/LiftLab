package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository // ASSUMED DOMAIN REPO
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.ProgramFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel // ASSUMED MAPPERS
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc // ASSUMED MAPPERS
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class ProgramsDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val programsRepository: ProgramsRepository // Depends on the DOMAIN repository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.PROGRAMS_COLLECTION
    override val firestoreDocClass: KClass<ProgramFirestoreDoc> = ProgramFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<ProgramFirestoreDoc> {
        return programsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<ProgramFirestoreDoc> {
        return programsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<ProgramFirestoreDoc>> {
        return programsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }
    
    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<ProgramFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            programsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<ProgramFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            programsRepository.updateMany(models)
        }
    }
    
    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        // Programs have no parents, so this is a no-op.
        return emptyList()
    }
}