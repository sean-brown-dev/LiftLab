package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.CustomLiftSetFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore

class CustomLiftSetsSyncRepository(
    private val dao: CustomSetsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<CustomLiftSetFirestoreDto, CustomLiftSet>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.CUSTOM_LIFT_SETS_COLLECTION,
    userId = userId
) {
    override suspend fun getAll(): List<CustomLiftSetFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }
}