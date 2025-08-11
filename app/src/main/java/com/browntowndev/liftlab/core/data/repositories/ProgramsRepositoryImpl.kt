package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.data.local.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.local.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.data.local.entities.ProgramEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgramsRepositoryImpl(
    private val programsDao: ProgramsDao,
    private val restTimerInProgressDao: RestTimerInProgressDao,
    private val syncScheduler: SyncScheduler,
) : ProgramsRepository {
    override suspend fun getAll(): List<Program> {
        return programsDao.getAll().map { it.toDomainModel() }
    }

    override fun getAllFlow(): Flow<List<Program>> {
        return programsDao.getAllWithRelationshipsFlow().map { programEntities ->
            programEntities.map { it.toDomainModel() }
        }
    }

    override suspend fun getById(id: Long): Program? {
        return programsDao.getWithRelationships(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<Program> {
        return programsDao.getMany(ids).fastMap { it.toDomainModel() }
    }

    override suspend fun getActive(): Program? {
        return programsDao.getActiveWithRelationships()?.let { programEntity ->
            val program = programEntity.toDomainModel()
            getSortedCopy(program)
        }
    }

    override fun getActiveProgramFlow(): Flow<Program?> {
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

    override suspend fun updateName(id: Long, newName: String) {
        val current = programsDao.get(id) ?: return
        val toUpdate = current.copy(name = newName).applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    override suspend fun updateDeloadWeek(id: Long, newDeloadWeek: Int) {
        val current = programsDao.get(id) ?: return
        val toUpdate = current.copy(deloadWeek = newDeloadWeek).applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    override suspend fun update(model: Program) {
        val current = programsDao.get(model.id) ?: return
        val toUpdate = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false
        )
        programsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    private suspend fun updateWithoutRefetch(programEntity: ProgramEntity) {
        programsDao.update(programEntity)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<Program>) {
        val currentEntities = programsDao.getMany(models.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate =
            models.fastMapNotNull { program ->
                val current = currentEntities[program.id]
                if (current == null) return@fastMapNotNull null
                program.toEntity().applyRemoteStorageMetadata(
                    remoteId = current.remoteId,
                    remoteLastUpdated = current.remoteLastUpdated,
                    synced = false
                )
            }
        programsDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: Program): Long {
        val toUpsert = model.toEntity()
        val id = programsDao.upsert(toUpsert)
        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<Program>): List<Long> {
        val toUpsert = models.map { it.toEntity() }
        val ids = programsDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, id) ->
            if (id == -1L) entity else entity.copy(id = id)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun insert(model: Program): Long {
        val toInsert = model.toEntity()
        val id = programsDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun insertMany(models: List<Program>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val ids = programsDao.insertMany(toInsert)
        syncScheduler.scheduleSync()

        return ids
    }

    override suspend fun getDeloadWeek(id: Long): Int {
        return programsDao.getDeloadWeek(id)
    }

    override fun getActiveProgramMetadataFlow(): Flow<ActiveProgramMetadata?> {
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

    override suspend fun deleteById(id: Long): Int {
        val toDelete = programsDao.get(id) ?: return 0
        val deletedProgramCount = programsDao.softDelete(id)
        if (deletedProgramCount > 0) {
            if (toDelete.isActive) {
                restTimerInProgressDao.deleteAll()
                val allPrograms = programsDao.getAll()
                if (allPrograms.isNotEmpty()) {
                    var newActiveProgram = allPrograms.first()
                    newActiveProgram = newActiveProgram.copy(isActive = true).applyRemoteStorageMetadata(
                        remoteId = newActiveProgram.remoteId,
                        remoteLastUpdated = newActiveProgram.remoteLastUpdated,
                        synced = false,
                    )
                    programsDao.update(newActiveProgram)
                }
            }
            syncScheduler.scheduleSync()
        }
        return deletedProgramCount
    }

    override suspend fun delete(model: Program): Int {
        return deleteById(model.id)
    }

    override suspend fun deleteMany(models: List<Program>): Int {
        val toDeleteIds = models.map { it.id }
        if (toDeleteIds.isEmpty()) return 0
        val deletedProgramCount = programsDao.softDeleteMany(toDeleteIds)
        if (deletedProgramCount > 0) {
            if (models.any { it.isActive }) {
                val allPrograms = programsDao.getAll()
                val hasActiveProgram = allPrograms.any { it.isActive }
                if (!hasActiveProgram && allPrograms.isNotEmpty()) {
                    var newActiveProgram = allPrograms.first()
                    newActiveProgram = newActiveProgram.copy(isActive = true).applyRemoteStorageMetadata(
                        remoteId = newActiveProgram.remoteId,
                        remoteLastUpdated = newActiveProgram.remoteLastUpdated,
                        synced = false,
                    )
                    programsDao.update(newActiveProgram)
                }
            }
            syncScheduler.scheduleSync()
        }
        return deletedProgramCount
    }

    override suspend fun updateMesoAndMicroCycle(id: Long, mesoCycle: Int, microCycle: Int, microCyclePosition: Int) {
        val current = programsDao.get(id) ?: return
        val toUpdate = current.copy(
            currentMesocycle = mesoCycle,
            currentMicrocycle = microCycle,
            currentMicrocyclePosition = microCyclePosition
        ).applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        programsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }
}