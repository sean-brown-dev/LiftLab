package com.browntowndev.liftlab.core.persistence.repositories.firestore

import android.util.Log
import com.browntowndev.liftlab.core.persistence.dtos.firestore.BaseFirestoreDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.browntowndev.liftlab.core.persistence.dao.BaseDao
import com.browntowndev.liftlab.core.persistence.entities.BaseEntity
import com.google.firebase.auth.FirebaseAuth


abstract class BaseSyncRepository<D : BaseFirestoreDto, E: BaseEntity>(
    private val dao: BaseDao<E>,
    private val toEntity: (D) -> E,
    private val firestore: FirebaseFirestore,
    val collectionName: String,
    private val firebaseAuth: FirebaseAuth,
) {
    val collection: CollectionReference
        get() = firebaseAuth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).collection(collectionName)
        } ?: throw IllegalStateException("User not authenticated")

    abstract suspend fun getAll(): List<D>

    abstract suspend fun getMany(ids: List<Long>): List<D>

    open suspend fun updateMany(dtos: List<D>) =
        dao.updateMany(dtos.map(toEntity))

    open suspend fun upsertMany(dtos: List<D>): List<Long> =
        dao.upsertMany(dtos.map(toEntity))

    open suspend fun deleteMany(dtos: List<D>) =
        dao.deleteMany(dtos.map(toEntity))

    open suspend fun delete(dto: D) =
        dao.delete(toEntity(dto))
}
