package com.browntowndev.liftlab.core.persistence.firestore.sync.datasources

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.domain.repositories.LiftMetricChartsRepository
import com.browntowndev.liftlab.core.persistence.firestore.FirestoreClient
import com.browntowndev.liftlab.core.persistence.firestore.documents.BaseFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.documents.LiftMetricChartFirestoreDoc
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncableDataSource
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
    override val firestoreDocClass: KClass<LiftMetricChartFirestoreDoc> = LiftMetricChartFirestoreDoc::class
    override val collection: CollectionReference get() = firestoreClient.userCollection(collectionName)

    override suspend fun getMany(roomEntityIds: List<Long>): List<LiftMetricChartFirestoreDoc> {
        return liftMetricChartsRepository.getManyByIds(roomEntityIds).map { it.toFirestoreDoc() }
    }

    override suspend fun getAll(): List<LiftMetricChartFirestoreDoc> {
        return liftMetricChartsRepository.getAll().map { it.toFirestoreDoc() }
    }

    override fun getAllFlow(): Flow<List<LiftMetricChartFirestoreDoc>> {
        return liftMetricChartsRepository.getAllFlow().map { domainModels ->
            domainModels.map { it.toFirestoreDoc() }
        }
    }

    override suspend fun upsertMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<LiftMetricChartFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            liftMetricChartsRepository.upsertMany(models)
        }
    }

    override suspend fun updateMany(entities: List<BaseFirestoreDoc>) {
        val models = entities.filterIsInstance<LiftMetricChartFirestoreDoc>().map { it.toDomainModel() }
        if (models.isNotEmpty()) {
            liftMetricChartsRepository.updateMany(models)
        }
    }

    override suspend fun getFirestoreIdsForEntitiesWithDeletedParents(entities: List<BaseFirestoreDoc>): List<String> {
        return emptyList()
    }
}