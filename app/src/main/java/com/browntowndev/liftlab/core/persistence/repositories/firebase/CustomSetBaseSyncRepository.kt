package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.CustomLiftSetFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.CustomLiftSet
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.FirebaseFirestore

class CustomSetBaseSyncRepository(
    private val dao: CustomSetsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<CustomLiftSetFirebaseDto, CustomLiftSet>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "customLiftSets",
    userId = userId
) {
    override suspend fun getAll(): List<CustomLiftSetFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}