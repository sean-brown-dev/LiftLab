package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.VolumeMetricChartsRepository
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.VolumeMetricChartFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
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
    override val firestoreDocClass: KClass<VolumeMetricChartFirestoreDoc> = VolumeMetricChartFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<VolumeMetricChartFirestoreDoc> {
        return volumeMetricChartsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<VolumeMetricChartFirestoreDoc> {
        return volumeMetricChartsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<VolumeMetricChartFirestoreDoc>> {
        return volumeMetricChartsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<VolumeMetricChartFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            volumeMetricChartsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<VolumeMetricChartFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            volumeMetricChartsRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        return emptyList()
    }
}