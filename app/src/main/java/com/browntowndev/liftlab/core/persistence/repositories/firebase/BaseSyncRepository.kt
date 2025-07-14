package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dtos.firebase.BaseFirebaseDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.browntowndev.liftlab.BuildConfig
import com.browntowndev.liftlab.core.persistence.dao.BaseDao
import com.browntowndev.liftlab.core.persistence.entities.BaseEntity


abstract class BaseSyncRepository<T : BaseFirebaseDto, E: BaseEntity>(
    private val dao: BaseDao<E>,
    private val toEntity: (T) -> E,
    private val firestore: FirebaseFirestore,
    releaseCollectionName: String,
    private val userId: String,
) {
    val collectionName = if (BuildConfig.DEBUG) "debug_$releaseCollectionName" else releaseCollectionName

    val collection: CollectionReference
        get() = firestore.collection("users").document(userId).collection(collectionName)

    abstract suspend fun getAll(): List<T>

    open suspend fun updateMany(dtos: List<T>) =
        dao.updateMany(dtos.map { dto -> toEntity(dto) })

    open suspend fun upsertMany(dtos: List<T>) {
        val entities = dtos.map(toEntity)
        val insertedIds = dao.insertManyIgnoreConflicts(entities)

        val failedUpdates = entities.zip(insertedIds)
            .filter { it.second == -1L }
            .map { it.first }

        if (failedUpdates.isNotEmpty()) {
            dao.updateMany(failedUpdates)
        }
    }
}
