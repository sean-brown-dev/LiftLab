package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.sync.BatchSyncQueueEntry
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.sync.SyncQueueEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProgramsRepository(
    private val programsDao: ProgramsDao,
    private val programMapper: ProgramMapper,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val firestoreSyncManager: FirestoreSyncManager,
) : Repository {
    suspend fun getAll(): List<ProgramDto> {
        return programsDao.getAll().map { programMapper.map(it) }
    }

    fun getAllFlow(): Flow<List<ProgramDto>> {
        return programsDao.getAllWithRelationshipsFlow().map { programEntities ->
            programEntities.map { programMapper.map(it) }
        }
    }

    suspend fun getActive(): ProgramDto? {
        return programsDao.getActiveWithRelationships()?.let { programEntity ->
            val program = programMapper.map(programEntity)
            getSortedCopy(program)
        }
    }

    fun getActiveProgramFlow(): Flow<ProgramDto?> {
        val programMeta = programsDao.getActiveWithRelationshipsFlow().map { programEntity ->
            if (programEntity != null) {
                val program = programMapper.map(programEntity)
                getSortedCopy(program)
            } else null
        }

        return programMeta
    }

    private fun getSortedCopy(program: ProgramDto): ProgramDto {
        return program.copy(workouts = program.workouts
            .sortedBy { workout -> workout.position }
            .map { workout ->
                workout.copy(lifts = workout.lifts
                    .sortedBy { lift -> lift.position }
                    .map { lift ->
                        when (lift) {
                            is CustomWorkoutLiftDto -> lift.copy(
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

    suspend fun update(program: ProgramDto) {
        val current = programsDao.get(program.id) ?: return
        val toUpdate = programMapper.map(program).applyFirestoreMetadata(
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

    private suspend fun updateWithoutRefetch(program: Program) {
        programsDao.update(program)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                roomEntityIds = listOf(program.id),
                SyncType.Upsert,
            )
        )
    }

    suspend fun updateMany(programs: List<ProgramDto>) {
        val currentEntities = programsDao.getMany(programs.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate =
            programs.fastMapNotNull { program ->
                val current = currentEntities[program.id]
                if (current == null) return@fastMapNotNull null
                programMapper.map(program).applyFirestoreMetadata(
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

    suspend fun insert(program: ProgramDto): Long {
        val toInsert = programMapper.map(program)
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

    fun getActiveProgramMetadataFlow(): Flow<ActiveProgramMetadataDto?> {
        return programsDao.getActiveProgramMetadata()
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

    suspend fun delete(programToDelete: ProgramDto) {
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