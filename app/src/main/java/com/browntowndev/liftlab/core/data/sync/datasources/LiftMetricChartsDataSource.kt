package com.browntowndev.liftlab.core.data.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.data.remote.FirestoreClient
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.LiftMetricChartRemoteDto
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class LiftMetricChartsDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val liftMetricChartsRepository: LiftMetricChartsRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.LIFT_METRIC_CHARTS_COLLECTION
    override val firestoreDocClass: KClass<LiftMetricChartRemoteDto> = LiftMetricChartRemoteDto::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<LiftMetricChartRemoteDto> {
        return liftMetricChartsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<LiftMetricChartRemoteDto> {
        return liftMetricChartsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<LiftMetricChartRemoteDto>> {
        return liftMetricChartsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<LiftMetricChartRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            liftMetricChartsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<LiftMetricChartRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            liftMetricChartsRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseRemoteDto>): List<String> {
        return emptyList()
    }
}