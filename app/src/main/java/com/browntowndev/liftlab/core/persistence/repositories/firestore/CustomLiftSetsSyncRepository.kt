package com.browntowndev.liftlab.core.persistence.repositories.firestore

import android.util.Log
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.CustomLiftSetFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomLiftSetsSyncRepository(
    private val dao: CustomSetsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<CustomLiftSetFirestoreDto, CustomLiftSet>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
    firebaseAuth = firebaseAuth
) {
    override suspend fun getAll(): List<CustomLiftSetFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<CustomLiftSetFirestoreDto> =
        dao.getMany(ids).map {
            Log.d("CustomLiftSetsSyncRepository", "getMany: $it")
            val firestoreLift = it.toFirestoreDto()
            Log.d("CustomLiftSetsSyncRepository", "getMany: $firestoreLift")
            firestoreLift
        }

    fun getAllFlow(): Flow<List<CustomLiftSetFirestoreDto>> =
        dao.getAllFlow().map { sets ->
            sets.map { set -> set.toFirestoreDto() }
        }
}