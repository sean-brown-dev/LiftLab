package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ProgramsRepository(
    private val programsDao: ProgramsDao,
    private val programMapper: ProgramMapper,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val syncScope: CoroutineScope,
) : Repository {
    suspend fun getAll(): List<ProgramDto> {
        return programsDao.getAll().map { programMapper.map(it) }
    }

    suspend fun getActiveNotAsLiveData(): ProgramDto? {
        return programsDao.getActiveNotAsLiveData()?.let { programEntity ->
            val program = programMapper.map(programEntity)
            getSortedCopy(program)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getActive(): LiveData<ProgramDto?> {
        val programMeta = programsDao.getActive().flatMapLatest { programEntity ->
            flowOf(
                if (programEntity != null) {
                    val program = programMapper.map(programEntity)
                    getSortedCopy(program)
                } else null
            )
        }.asLiveData()

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
        val toUpdate = programsDao.get(id)?.copy(name = newName) ?: return
        updateWithoutRefetch(toUpdate)
    }

    suspend fun updateDeloadWeek(id: Long, newDeloadWeek: Int) {
        val toUpdate = programsDao.get(id)?.copy(deloadWeek = newDeloadWeek) ?: return
        updateWithoutRefetch(toUpdate)
    }

    suspend fun update(program: ProgramDto) {
        val current = programsDao.get(program.id) ?: return
        val toUpdate = programMapper.map(program).copyWithFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false
        )
        programsDao.update(toUpdate)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                entity = toUpdate.toFirestoreDto(),
                onSynced = {
                    programsDao.update(it.toEntity())
                }
            )
        }
    }

    private suspend fun updateWithoutRefetch(program: Program) {
        program.synced = false
        programsDao.update(program)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                entity = program.toFirestoreDto(),
                onSynced = {
                    programsDao.update(it.toEntity())
                }
            )
        }
    }

    suspend fun updateMany(programs: List<ProgramDto>) {
        val currentEntities = programsDao.getMany(programs.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate =
            programs.fastMap { program ->
                val current = currentEntities[program.id]
                programMapper.map(program).copyWithFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false
                )
            }
        programsDao.updateMany(toUpdate)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncMany(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                entities = toUpdate.map { it.toFirestoreDto() },
                onSynced = { syncedEntities ->
                    programsDao.updateMany(syncedEntities.map { it.toEntity() })
                }
            )
        }
    }

    suspend fun insert(program: ProgramDto): Long {
        val toInsert = programMapper.map(program)
        val id = programsDao.insert(toInsert)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                entity = toInsert.toFirestoreDto().copy(id = id),
                onSynced = {
                    programsDao.update(it.toEntity())
                }
            )
        }

        return id
    }

    suspend fun getDeloadWeek(id: Long): Int {
        return programsDao.getDeloadWeek(id)
    }

    fun getActiveProgramMetadata(): LiveData<ActiveProgramMetadataDto?> {
        return programsDao.getActiveProgramMetadata().asLiveData()
    }

    suspend fun delete(id: Long) {
        val toDelete = programsDao.get(id) ?: return
        programsDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            syncScope.fireAndForgetSync {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                    firestoreId = toDelete.firestoreId!!
                )
            }
        }
    }

    suspend fun delete(programToDelete: ProgramDto) {
        delete(programToDelete.id)
    }

    suspend fun updateMesoAndMicroCycle(id: Long, mesoCycle: Int, microCycle: Int, microCyclePosition: Int) {
        val toUpdate = programsDao.get(id)?.copy(
            currentMesocycle = mesoCycle,
            currentMicrocycle = microCycle,
            currentMicrocyclePosition = microCyclePosition) ?: return
        programsDao.update(toUpdate)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.PROGRAMS_COLLECTION,
                entity = toUpdate.toFirestoreDto(),
                onSynced = {
                    programsDao.update(it.toEntity())
                }
            )
        }
    }
}