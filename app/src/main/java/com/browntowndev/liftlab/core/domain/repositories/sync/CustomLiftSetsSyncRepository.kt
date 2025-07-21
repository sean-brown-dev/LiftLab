package com.browntowndev.liftlab.core.domain.repositories.sync

import android.util.Log
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.room.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.firestore.entities.CustomLiftSetFirestoreEntity
import com.browntowndev.liftlab.core.persistence.entities.room.CustomLiftSetEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomLiftSetsSyncRepository(
    private val dao: CustomSetsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<CustomLiftSetFirestoreEntity, CustomLiftSetEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
    firebaseAuth = firebaseAuth
) {
    override suspend fun getAll(): List<CustomLiftSetFirestoreEntity> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<CustomLiftSetFirestoreEntity> =
        dao.getMany(ids).map {
            Log.d("CustomLiftSetsSyncRepository", "getMany: $it")
            val firestoreLift = it.toFirestoreDto()
            Log.d("CustomLiftSetsSyncRepository", "getMany: $firestoreLift")
            firestoreLift
        }

    fun getAllFlow(): Flow<List<CustomLiftSetFirestoreEntity>> =
        dao.getAllFlow().map { sets ->
            sets.map { set -> set.toFirestoreDto() }
        }
}