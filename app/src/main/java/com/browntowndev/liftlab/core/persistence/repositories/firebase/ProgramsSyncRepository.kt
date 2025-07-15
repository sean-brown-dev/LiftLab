package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.ProgramFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore

class ProgramsSyncRepository(
    private val dao: ProgramsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<ProgramFirestoreDto, Program>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
    userId = userId,
) {
    override suspend fun getAll(): List<ProgramFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }
}