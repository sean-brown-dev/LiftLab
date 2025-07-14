package com.browntowndev.liftlab.core.persistence.repositories.firebase

import com.browntowndev.liftlab.core.persistence.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.persistence.dtos.firebase.RestTimerInProgressFirebaseDto
import com.browntowndev.liftlab.core.persistence.dtos.firebase.SetLogEntryFirebaseDto
import com.browntowndev.liftlab.core.persistence.entities.SetLogEntry
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class SetLogEntryBaseSyncRepository(
    private val dao: SetLogEntryDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<SetLogEntryFirebaseDto, SetLogEntry>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    releaseCollectionName = "setLogEntries",
    userId = userId,
) {
    override suspend fun getAll(): List<SetLogEntryFirebaseDto> =
        dao.getAll().map { it.toFirebaseDto() }
}