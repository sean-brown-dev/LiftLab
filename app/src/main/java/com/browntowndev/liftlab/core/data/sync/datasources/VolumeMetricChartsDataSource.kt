package com.browntowndev.liftlab.core.data.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import com.browntowndev.liftlab.core.data.remote.FirestoreClient
import com.browntowndev.liftlab.core.data.remote.dto.BaseRemoteDto
import com.browntowndev.liftlab.core.data.remote.dto.VolumeMetricChartRemoteDto
import com.browntowndev.liftlab.core.common.mapping.toDomainModel
import com.browntowndev.liftlab.core.common.mapping.toFirestoreDoc
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class VolumeMetricChartsDataSource @Inject constructor(
    private val firestoreClient: FirestoreClient,
    private val volumeMetricChartsRepository: VolumeMetricChartsRepository
) : SyncableDataSource {

    override val collectionName: String = FirestoreConstants.VOLUME_METRIC_CHARTS_COLLECTION
    override val firestoreDocClass: KClass<VolumeMetricChartRemoteDto> = VolumeMetricChartRemoteDto::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<VolumeMetricChartRemoteDto> {
        return volumeMetricChartsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<VolumeMetricChartRemoteDto> {
        return volumeMetricChartsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<VolumeMetricChartRemoteDto>> {
        return volumeMetricChartsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<VolumeMetricChartRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            volumeMetricChartsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseRemoteDto>) {
        val models = entities.filterIsInstance<VolumeMetricChartRemoteDto>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            volumeMetricChartsRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseRemoteDto>): List<String> {
        return emptyList()
    }
}