package com.browntowndev.liftlab.core.persistence.repositories.firebase

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.persistence.dtos.firestore.BaseFirestoreDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.browntowndev.liftlab.core.persistence.dao.BaseDao
import com.browntowndev.liftlab.core.persistence.entities.BaseEntity


abstract class BaseSyncRepository<D : BaseFirestoreDto, E: BaseEntity>(
    private val dao: BaseDao<E>,
    private val toEntity: (D) -> E,
    private val firestore: FirebaseFirestore,
    val collectionName: String,
    private val userId: String,
) {
    val collection: CollectionReference
        get() = firestore.collection("users").document(userId).collection(collectionName)

    abstract suspend fun getAll(): List<D>

    open suspend fun updateMany(dtos: List<D>) =
        dao.updateMany(dtos.map(toEntity))

    open suspend fun upsertMany(dtos: List<D>): List<Long> =
        dao.upsertMany(dtos.map(toEntity))
}
