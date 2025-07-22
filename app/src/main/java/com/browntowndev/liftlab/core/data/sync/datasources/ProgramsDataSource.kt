package com.browntowndev.liftlab.core.data.sync.datasources

import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository // ASSUMED DOMAIN REPO
import com.browntowndev.liftlab.core.data.remote.FirestoreClient
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.ProgramRemoteDto
import com.browntowndev.liftlab.core.common.mapping.toDomainModel // ASSUMED MAPPERS
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc // ASSUMED MAPPERS
import com.browntowndev.liftlab.core.data.common.RemoteCollectionNames
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProgramsDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val programsRepository: ProgramsRepository // Depends on the DOMAIN repository
) : SyncableDataSource {

    override val collectionName: String = RemoteCollectionNames.PROGRAMS_COLLECTION

    override suspend fun getMany(roomEntityIds: List<Long>): List<ProgramRemoteDto> {
        return programsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<ProgramRemoteDto> {
        return programsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<ProgramRemoteDto>> {
        return programsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }
    
    override suspend fun upsertMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<ProgramRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            programsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<ProgramRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            programsRepository.updateMany(models)
        }
    }
    
    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseRemoteDto>): List<String> {
        // Programs have no parents, so this is a no-op.
        return emptyList()
    }
}