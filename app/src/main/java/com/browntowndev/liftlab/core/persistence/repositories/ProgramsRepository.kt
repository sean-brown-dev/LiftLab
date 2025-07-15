package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.entities.Program
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ProgramsRepository(
    private val programsDao: ProgramsDao,
    private val programMapper: ProgramMapper,
    private val firestoreSyncManager: FirestoreSyncManager,
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
        programsDao.get(id)?.copy(name = newName)?.let { program ->
            updateWithoutRefetch(program)
        }
    }

    suspend fun updateDeloadWeek(id: Long, newDeloadWeek: Int) {
        programsDao.get(id)?.copy(deloadWeek = newDeloadWeek)?.let { program ->
            updateWithoutRefetch(program)
        }
    }

    suspend fun update(program: ProgramDto) {
        val current = programsDao.get(program.id)
        val toUpdate = programMapper.map(program).copyWithFirestoreMetadata(
            firestoreId = current?.firestoreId,
            lastUpdated = current?.lastUpdated,
            synced = false
        )
        programsDao.update(toUpdate)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.PROGRAMS_COLLECTION,
            entity = toUpdate.toFirebaseDto(),
            onSynced = {
                programsDao.update(it.toEntity())
            }
        )
    }

    private suspend fun updateWithoutRefetch(program: Program) {
        program.synced = false
        programsDao.update(program)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.PROGRAMS_COLLECTION,
            entity = program.toFirebaseDto(),
            onSynced = {
                programsDao.update(it.toEntity())
            }
        )
    }

    suspend fun updateMany(programs: List<ProgramDto>) {
        val currentEntities = programsDao.getMany(programs.map { it.id }).associateBy { it.id }
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
        firestoreSyncManager.syncMany(
            collectionName = FirebaseConstants.PROGRAMS_COLLECTION,
            entities = toUpdate.map { it.toFirebaseDto() },
            onSynced = { syncedEntities ->
                programsDao.updateMany(syncedEntities.map { it.toEntity() })
            }
        )
    }

    suspend fun insert(program: ProgramDto): Long {
        val toInsert = programMapper.map(program)
        val id = programsDao.insert(toInsert)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.PROGRAMS_COLLECTION,
            entity = toInsert.toFirebaseDto().copy(id = id),
            onSynced = {
                programsDao.update(it.toEntity())
            }
        )

        return id
    }

    suspend fun getDeloadWeek(id: Long): Int {
        return programsDao.getDeloadWeek(id)
    }

    fun getActiveProgramMetadata(): LiveData<ActiveProgramMetadataDto?> {
        return programsDao.getActiveProgramMetadata().asLiveData()
    }

    suspend fun delete(id: Long) {
        programsDao.get(id)?.let { program ->
            programsDao.delete(program)

            if (program.firestoreId != null) {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirebaseConstants.PROGRAMS_COLLECTION,
                    firestoreId = program.firestoreId!!
                )
            }
        }
    }

    suspend fun delete(programToDelete: ProgramDto) {
        delete(programToDelete.id)
    }

    suspend fun updateMesoAndMicroCycle(id: Long, mesoCycle: Int, microCycle: Int, microCyclePosition: Int) {
        programsDao.updateMesoAndMicroCycle(id, mesoCycle, microCycle, microCyclePosition)
        val updated = programsDao.get(id)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.PROGRAMS_COLLECTION,
            entity = updated!!.toFirebaseDto(),
            onSynced = {
                programsDao.update(it.toEntity())
            }
        )
    }
}