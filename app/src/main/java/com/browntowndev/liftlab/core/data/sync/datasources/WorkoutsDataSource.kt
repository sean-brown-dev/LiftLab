package com.browntowndev.liftlab.core.data.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.data.remote.FirestoreClient
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.WorkoutRemoteDto
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
    override val firestoreDocClass: KClass<WorkoutRemoteDto> = WorkoutRemoteDto::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<WorkoutRemoteDto> {
        return workoutsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<WorkoutRemoteDto> {
        return workoutsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<WorkoutRemoteDto>> {
        return workoutsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }
    
    override suspend fun upsertMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<WorkoutRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<WorkoutRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            workoutsRepository.updateMany(models)
        }
    }
    
    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseRemoteDto>): List<String> {
        val workoutFirestoreDtos = entities.filterIsInstance<WorkoutRemoteDto>()
        val workoutProgramIds = workoutFirestoreDtos.map { it.programId }.distinct()
        val programIds = programsRepository.getManyByIds(workoutProgramIds)
            .map { it.id }
            .toHashSet()

        return workoutFirestoreDtos.mapNotNull { dto ->
            if (dto.programId !in programIds && dto.remoteId != null) {
                dto.remoteId
            } else null
        }
    }
}