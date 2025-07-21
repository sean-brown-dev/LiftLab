package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutInProgressFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class WorkoutInProgressDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val workoutsRepository: WorkoutsRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.WORKOUT_IN_PROGRESS_COLLECTION
    override val firestoreDocClass: KClass<WorkoutInProgressFirestoreDoc> = WorkoutInProgressFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<WorkoutInProgressFirestoreDoc> {
        return workoutInProgressRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<WorkoutInProgressFirestoreDoc> {
        return workoutInProgressRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<WorkoutInProgressFirestoreDoc>> {
        return workoutInProgressRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<WorkoutInProgressFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutInProgressRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<WorkoutInProgressFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutInProgressRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        val workoutInProgressFirestoreDtos = entities.filterIsInstance<WorkoutInProgressFirestoreDoc>()
        val workoutInProgressWorkoutIds = workoutInProgressFirestoreDtos.map { it.workoutId }.distinct()
        val workoutIds = workoutsRepository.getManyByIds(workoutInProgressWorkoutIds)
            .map { it.id }
            .toHashSet()

        return workoutInProgressFirestoreDtos.mapNotNull { dto ->
            if (dto.workoutId !in workoutIds && dto.firestoreId != null) {
                dto.firestoreId
            } else null
        }
    }
}