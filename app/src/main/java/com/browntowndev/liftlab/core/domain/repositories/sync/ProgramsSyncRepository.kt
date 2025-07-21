package com.browntowndev.liftlab.core.domain.repositories.sync

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.room.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.firestore.documents.ProgramFirestoreDoc
import com.browntowndev.liftlab.core.persistence.room.entities.ProgramEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.FirestoreMappingExtensions.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ProgramsSyncRepository(
    private val dao: ProgramsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<ProgramFirestoreDoc, ProgramEntity>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
    firebaseAuth = firebaseAuth,
), KoinComponent {
    override suspend fun getAll(): List<ProgramFirestoreDoc> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<ProgramFirestoreDoc> =
        dao.getMany(ids).map { it.toFirestoreDto() }

    override suspend fun updateMany(dtos: List<ProgramFirestoreDoc>) {
        super.updateMany(dtos)

        // Deactivate all other programs
        val activeProgramFromUpdate = dtos.firstOrNull { it.isActive }
        setOnlyOneProgramAsActive(activeProgramFromUpdate)
    }

    override suspend fun upsertMany(dtos: List<ProgramFirestoreDoc>): List<Long> {
        val upsertIds = super.upsertMany(dtos)

        // Pick one active programEntity from the upsert, if it exists
        val activeProgramFromUpsert = dtos.zip(upsertIds)
            .filter { it.first.isActive }
            .map { (dto, id) ->
                if (id == -1L) dto else dto.copy(id = id)
            }.firstOrNull()

        // Deactivate all other programs
        setOnlyOneProgramAsActive(activeProgramFromUpsert)

        return upsertIds
    }

    private suspend fun setOnlyOneProgramAsActive(activeProgramFromUpsert: ProgramFirestoreDoc?) {
        if (activeProgramFromUpsert != null) {
            dao.getAllActive().fastForEach { program ->
                if (program.id != activeProgramFromUpsert.id) {
                    val deactivatedProgram = program.copy(isActive = false).apply {
                        this.firestoreId = program.firestoreId
                        this.lastUpdated = program.lastUpdated
                        this.synced = false
                    }
                    dao.update(deactivatedProgram)

                    val firestoreSyncManager: FirestoreSyncManager by inject()
                    firestoreSyncManager.enqueueSyncRequest(
                        SyncQueueEntry(
                            collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                            roomEntityIds = listOf(deactivatedProgram.id),
                            syncType = SyncType.Upsert,
                        )
                    )
                }
            }
        }
    }
}