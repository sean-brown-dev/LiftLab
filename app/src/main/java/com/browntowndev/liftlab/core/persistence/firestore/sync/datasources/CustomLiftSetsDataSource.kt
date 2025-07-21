package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.CustomLiftSetFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreClient
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class CustomLiftSetsDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION
    override val firestoreDocClass: KClass<CustomLiftSetFirestoreDoc> = CustomLiftSetFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<CustomLiftSetFirestoreDoc> {
        return customLiftSetsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<CustomLiftSetFirestoreDoc> {
        return customLiftSetsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<CustomLiftSetFirestoreDoc>> {
        return customLiftSetsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<CustomLiftSetFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            customLiftSetsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<CustomLiftSetFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            customLiftSetsRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        val customLiftSetFirestoreDtos = entities.filterIsInstance<CustomLiftSetFirestoreDoc>()
        val customLiftSetWorkoutIds = customLiftSetFirestoreDtos.map { it.workoutLiftId }.distinct()
        val workoutLiftIds = workoutLiftsRepository.getManyByIds(customLiftSetWorkoutIds)
            .map { it.id }
            .toHashSet()

        return customLiftSetFirestoreDtos.mapNotNull { dto ->
            if (dto.workoutLiftId !in workoutLiftIds && dto.firestoreId != null) {
                dto.firestoreId
            } else null
        }
    }
}