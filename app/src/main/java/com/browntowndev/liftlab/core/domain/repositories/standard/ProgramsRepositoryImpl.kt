package com.browntowndev.liftlab.core.domain.repositories.standard

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.models.ActiveProgramMetadata
import com.browntowndev.liftlab.core.persistence.room.dao.ProgramsDao
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Program
import com.browntowndev.liftlab.core.persistence.entities.room.ProgramEntity
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.domain.mapping.ProgramMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.mapping.ProgramMappingExtensions.toEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.BatchSyncQueueEntry
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProgramsRepository(
    private val programsDao: ProgramsDao,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val firestoreSyncManager: FirestoreSyncManager,
) : Repository {
    suspend fun getAll(): List<Program> {
        return programsDao.getAll().map { it.toDomainModel() }
    }

    fun getAllFlow(): Flow<List<Program>> {
        return programsDao.getAllWithRelationshipsFlow().map { programEntities ->
            programEntities.map { it.toDomainModel() }
        }
    }

    suspend fun getActive(): Program? {
        return programsDao.getActiveWithRelationships()?.let { programEntity ->
            val program = programEntity.toDomainModel()
            getSortedCopy(program)
        }
    }

    fun getActiveProgramFlow(): Flow<Program?> {
        val programMeta = programsDao.getActiveWithRelationshipsFlow().map { programEntity ->
            if (programEntity != null) {
                val program = programEntity.toDomainModel()
                getSortedCopy(program)
            } else null
        }

        return programMeta
    }

    private fun getSortedCopy(program: Program): Program {
        return program.copy(workouts = program.workouts
            .sortedBy { workout -> workout.position }
            .map { workout ->
                workout.copy(lifts = workout.lifts
                    .sortedBy { lift -> lift.position }
                    .map { lift ->
                        when (lift) {
                            is CustomWorkoutLift -> lift.copy(
                                customLiftSets = lift.customLiftSets.sortedBy { it.position }
                            )
                            else -> lift
                        }
                    }
                )
            }
        )
    }

    suspend fun updateName(id: Long, newName: String) {
        val current = programsDao.get(id) ?: return
        val toUpdate = current.copy(name = newName).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    suspend fun updateDeloadWeek(id: Long, newDeloadWeek: Int) {
        val current = programsDao.get(id) ?: return
        val toUpdate = current.copy(deloadWeek = newDeloadWeek).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    suspend fun update(program: Program) {
        val current = programsDao.get(program.id) ?: return
        val toUpdate = program.toEntity().applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false
        )
        programsDao.update(toUpdate)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                roomEntityIds = listOf(toUpdate.id),
                SyncType.Upsert,
            )
        )
    }

    private suspend fun updateWithoutRefetch(programEntity: ProgramEntity) {
        programsDao.update(programEntity)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                roomEntityIds = listOf(programEntity.id),
                SyncType.Upsert,
            )
        )
    }

    suspend fun updateMany(programs: List<Program>) {
        val currentEntities = programsDao.getMany(programs.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate =
            programs.fastMapNotNull { program ->
                val current = currentEntities[program.id]
                if (current == null) return@fastMapNotNull null
                program.toEntity().applyFirestoreMetadata(
                    firestoreId = current.firestoreId,
                    lastUpdated = current.lastUpdated,
                    synced = false
                )
            }
        programsDao.updateMany(toUpdate)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                roomEntityIds = toUpdate.fastMap { it.id },
                SyncType.Upsert,
            )
        )
    }

    suspend fun insert(program: Program): Long {
        val toInsert = program.toEntity()
        val id = programsDao.insert(toInsert)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                roomEntityIds = listOf(program.id),
                SyncType.Upsert,
            )
        )

        return id
    }

    suspend fun getDeloadWeek(id: Long): Int {
        return programsDao.getDeloadWeek(id)
    }

    fun getActiveProgramMetadataFlow(): Flow<ActiveProgramMetadata?> {
        return programsDao.getActiveProgramMetadata().map { metadata ->
            if (metadata == null) return@map null
            ActiveProgramMetadata(
                programId = metadata.programId,
                name = metadata.name,
                deloadWeek = metadata.deloadWeek,
                currentMesocycle = metadata.currentMesocycle,
                currentMicrocycle = metadata.currentMicrocycle,
                currentMicrocyclePosition = metadata.currentMicrocyclePosition,
                workoutCount = metadata.workoutCount,
            )
        }
    }

    suspend fun delete(id: Long) {
        val toDelete = programsDao.get(id) ?: return
        programsDao.delete(toDelete)

        val syncQueueEntries = mutableListOf<SyncQueueEntry>()
        if (toDelete.firestoreId != null) {
            syncQueueEntries.add(
                SyncQueueEntry(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                roomEntityIds = listOf(toDelete.id),
                SyncType.Delete,
            ))
        }

        if (toDelete.isActive) {
            restTimerInProgressRepository.deleteAll()

            var newActiveProgram = programsDao.getAll().firstOrNull() ?: return
            newActiveProgram = newActiveProgram.copy(isActive = true).applyFirestoreMetadata(
                firestoreId = newActiveProgram.firestoreId,
                lastUpdated = newActiveProgram.lastUpdated,
                synced = false,
            )
            programsDao.update(newActiveProgram)
            syncQueueEntries.add(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                    roomEntityIds = listOf(newActiveProgram.id),
                    SyncType.Upsert,
                )
            )
        }

        if (syncQueueEntries.size == 1) {
            firestoreSyncManager.enqueueSyncRequest(
                syncQueueEntries.first()
            )
        } else if (syncQueueEntries.size > 1) {
            firestoreSyncManager.enqueueBatchSyncRequest(
                BatchSyncQueueEntry(
                    id = UUID.randomUUID().toString(),
                    batch = syncQueueEntries,
                )
            )
        }
    }

    suspend fun delete(programToDelete: Program) {
        delete(programToDelete.id)
    }

    suspend fun updateMesoAndMicroCycleAndGetSyncQueueEntry(id: Long, mesoCycle: Int, microCycle: Int, microCyclePosition: Int): SyncQueueEntry? {
        val current = programsDao.get(id) ?: return null
        val toUpdate = current.copy(
            currentMesocycle = mesoCycle,
            currentMicrocycle = microCycle,
            currentMicrocyclePosition = microCyclePosition
        ).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        programsDao.update(toUpdate)

        return SyncQueueEntry(
            collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
            roomEntityIds = listOf(toUpdate.id),
            SyncType.Upsert,
        )
    }

    suspend fun updateMesoAndMicroCycle(id: Long, mesoCycle: Int, microCycle: Int, microCyclePosition: Int) {
        val syncQueEntry = updateMesoAndMicroCycleAndGetSyncQueueEntry(id, mesoCycle, microCycle, microCyclePosition) ?: return
        firestoreSyncManager.enqueueSyncRequest(syncQueEntry)
    }
}