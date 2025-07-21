package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.WorkoutFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class WorkoutsDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val workoutsRepository: WorkoutsRepository,
    private val programsRepository: ProgramsRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.WORKOUTS_COLLECTION
    override val firestoreDocClass: KClass<WorkoutFirestoreDoc> = WorkoutFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<WorkoutFirestoreDoc> {
        return workoutsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<WorkoutFirestoreDoc> {
        return workoutsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<WorkoutFirestoreDoc>> {
        return workoutsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }
    
    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<WorkoutFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<WorkoutFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutsRepository.updateMany(models)
        }
    }
    
    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        val workoutFirestoreDtos = entities.filterIsInstance<WorkoutFirestoreDoc>()
        val workoutProgramIds = workoutFirestoreDtos.map { it.programId }.distinct()
        val programIds = programsRepository.getManyByIds(workoutProgramIds)
            .map { it.id }
            .toHashSet()

        return workoutFirestoreDtos.mapNotNull { dto ->
            if (dto.programId !in programIds && dto.firestoreId != null) {
                dto.firestoreId
            } else null
        }
    }
}