package com.browntowndev.liftlab.core.data.sync.datasources

import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.CustomLiftSetRemoteDto
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import com.browntowndev.liftlab.core.data.sync.FirestoreClient
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

    override val collectionName: String = RemoteCollectionNames.CUSTOM_LIFT_SETS_COLLECTION
    override val firestoreDocClass: KClass<CustomLiftSetRemoteDto> = CustomLiftSetRemoteDto::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<CustomLiftSetRemoteDto> {
        return customLiftSetsRepository.getMany(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<CustomLiftSetRemoteDto> {
        return customLiftSetsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<CustomLiftSetRemoteDto>> {
        return customLiftSetsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<CustomLiftSetRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            customLiftSetsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<CustomLiftSetRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            customLiftSetsRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseRemoteDto>): List<String> {
        val customLiftSetFirestoreDtos = entities.filterIsInstance<CustomLiftSetRemoteDto>()
        val customLiftSetWorkoutIds = customLiftSetFirestoreDtos.map { it.workoutLiftId }.distinct()
        val workoutLiftIds = workoutLiftsRepository.getManyByIds(customLiftSetWorkoutIds)
            .map { it.id }
            .toHashSet()

        return customLiftSetFirestoreDtos.mapNotNull { dto ->
            if (dto.workoutLiftId !in workoutLiftIds && dto.remoteId != null) {
                dto.remoteId
            } else null
        }
    }
}