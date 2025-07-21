package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutLiftFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class WorkoutLiftsDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val workoutsRepository: WorkoutsRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.WORKOUT_LIFTS_COLLECTION
    override val firestoreDocClass: KClass<WorkoutLiftFirestoreDoc> = WorkoutLiftFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<WorkoutLiftFirestoreDoc> {
        return workoutLiftsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<WorkoutLiftFirestoreDoc> {
        return workoutLiftsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<WorkoutLiftFirestoreDoc>> {
        return workoutLiftsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<WorkoutLiftFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutLiftsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<WorkoutLiftFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutLiftsRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        val workoutLiftFirestoreDtos = entities.filterIsInstance<WorkoutLiftFirestoreDoc>()
        val workoutLiftWorkoutIds = workoutLiftFirestoreDtos.map { it.workoutId }.distinct()
        val workoutIds = workoutsRepository.getManyByIds(workoutLiftWorkoutIds)
            .map { it.id }
            .toHashSet()

        return workoutLiftFirestoreDtos.mapNotNull { dto ->
            if (dto.workoutId !in workoutIds && dto.firestoreId != null) {
                dto.firestoreId
            } else null
        }
    }
}