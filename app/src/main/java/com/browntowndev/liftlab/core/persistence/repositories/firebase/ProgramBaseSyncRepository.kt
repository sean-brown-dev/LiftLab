package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.PreviousSetResultFirebaseDto
import com.browntowndev.liftlab.core.persistence.dtos.firebase.ProgramFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class ProgramBaseSyncRepository(
    private val dao: ProgramsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<ProgramFirebaseDto, Program>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "programs",
    userId = userId,
) {
    override suspend fun getAll(): List<ProgramFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}