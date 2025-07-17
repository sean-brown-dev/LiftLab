package com.browntowndev.liftlab.core.persistence.repositories.firebase

import android.util.Log
import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dtos.firestore.PreviousSetResultFirestoreDto
import com.browntowndev.liftlab.core.persistence.dtos.firestore.ProgramFirestoreDto
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

class ProgramsSyncRepository(
    private val dao: ProgramsDao,
    firestore: FirebaseFirestore,
    firebaseAuth: FirebaseAuth,
) : BaseSyncRepository<ProgramFirestoreDto, Program>(
    dao = dao,
    toEntity = { it.toEntity() },
    firestore = firestore,
    collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
    firebaseAuth = firebaseAuth,
), KoinComponent {
    override suspend fun getAll(): List<ProgramFirestoreDto> =
        dao.getAll().map { it.toFirestoreDto() }

    override suspend fun getMany(ids: List<Long>): List<ProgramFirestoreDto> =
        dao.getMany(ids).map { it.toFirestoreDto() }

    override suspend fun updateMany(dtos: List<ProgramFirestoreDto>) {
        super.updateMany(dtos)

        // Deactivate all other programs
        val activeProgramFromUpdate = dtos.firstOrNull { it.isActive }
        setOnlyOneProgramAsActive(activeProgramFromUpdate)
    }

    override suspend fun upsertMany(dtos: List<ProgramFirestoreDto>): List<Long> {
        val upsertIds = super.upsertMany(dtos)

        // Pick one active program from the upsert, if it exists
        val activeProgramFromUpsert = dtos.zip(upsertIds)
            .filter { it.first.isActive }
            .map { (dto, id) ->
                if (id == -1L) dto else dto.copy(id = id)
            }.firstOrNull()

        // Deactivate all other programs
        setOnlyOneProgramAsActive(activeProgramFromUpsert)

        return upsertIds
    }

    private suspend fun setOnlyOneProgramAsActive(activeProgramFromUpsert: ProgramFirestoreDto?) {
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
                    firestoreSyncManager.syncSingle(
                        collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                        entity = deactivatedProgram.toFirestoreDto(),
                        onSynced = {
                            dao.update(it.toEntity())
                            Log.d(
                                "ProgramsSyncRepository",
                                "deactivatedProgram: $deactivatedProgram, " +
                                        "baseProgram: (lastUpdated=${deactivatedProgram.lastUpdated}, firestoreId=${deactivatedProgram.firestoreId})"
                            )
                        }
                    )
                }
            }
        }
    }
}