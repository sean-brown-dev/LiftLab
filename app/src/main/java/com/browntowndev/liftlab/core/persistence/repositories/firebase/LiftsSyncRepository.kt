package com.browntowndev.liftlab.core.persistence.repositories.firebase

import android.util.Log
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.persistence.dao.LiftsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.LiftFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.Lift
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.google.firebase.firestore.FirebaseFirestore

class LiftsSyncRepository(
    private val dao: LiftsDao,
    firestore: FirebaseFirestore,
    userId: String
) : BaseSyncRepository<LiftFirestoreDto, Lift>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.LIFTS_COLLECTION,
    userId = userId
) {
    override suspend fun getAll(): List<LiftFirestoreDto> =
        dao.getAll(includeHidden = true).map { it.toFirestoreDto() }
}
