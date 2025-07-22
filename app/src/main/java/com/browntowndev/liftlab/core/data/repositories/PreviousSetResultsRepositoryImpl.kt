package com.browntowndev.liftlab.core.data.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.data.local.dtos.PersonalRecordDto
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.SetResultMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.SetResultMappingExtensions.toSetResult
import com.browntowndev.liftlab.core.data.sync.SyncScheduler
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreviousSetResultsRepositoryImpl(
    private val previousSetResultDao: PreviousSetResultDao,
    private val syncScheduler: SyncScheduler,
): PreviousSetResultsRepository {
    override suspend fun getByWorkoutIdExcludingGivenMesoAndMicroFlow(workoutId: Long, mesoCycle: Int, microCycle: Int): Flow<List<SetResult>> {
        return previousSetResultDao.getByWorkoutIdExcludingGivenMesoAndMicroFlow(
            workoutId,
            mesoCycle,
            microCycle
        ).map { setResults ->
            setResults.fastMap { it.toSetResult() }
        }
    }


    override suspend fun getForWorkoutFlow(workoutId: Long, mesoCycle: Int, microCycle: Int): Flow<List<SetResult>> {
        return previousSetResultDao.getForWorkoutFlow(workoutId, mesoCycle, microCycle).map { results ->
            results.fastMap { it.toSetResult() }
        }
    }

    override suspend fun getForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<SetResult> {
        return previousSetResultDao.getForWorkout(workoutId, mesoCycle, microCycle)
            .fastMap { it.toSetResult() }
    }

    override suspend fun getPersonalRecordsForLiftsExcludingWorkout(
        workoutId: Long,
        mesoCycle: Int, microCycle: Int,
        liftIds: List<Long>
    ): List<PersonalRecordDto> {
        return previousSetResultDao.getPersonalRecordsForLiftsExcludingWorkout(
            workoutId = workoutId,
            mesoCycle = mesoCycle,
            microCycle = microCycle,
            liftIds = liftIds,
        )
    }

    override suspend fun getAll(): List<SetResult> {
        return previousSetResultDao.getAll().map { it.toSetResult() }
    }

    override fun getAllFlow(): Flow<List<SetResult>> {
        return previousSetResultDao.getAllFlow().map { setResults -> setResults.fastMap { it.toSetResult() } }
    }

    override suspend fun getById(id: Long): SetResult? {
        return previousSetResultDao.get(id)?.toSetResult()
    }

    override suspend fun getMany(ids: List<Long>): List<SetResult> {
        return previousSetResultDao.getMany(ids).map { it.toSetResult() }
    }

    override suspend fun update(model: SetResult) {
        val toUpdate = model.toEntity()
        previousSetResultDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<SetResult>) {
        val toUpdate = models.map { it.toEntity() }
        previousSetResultDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: SetResult): Long {
        val current = previousSetResultDao.get(model.id)
        val toUpsert = model.toEntity()
            .applyRemoteStorageMetadata(
                remoteId = current?.remoteId,
                remoteLastUpdated = current?.lastUpdated,
                synced = false
            )
        val id = previousSetResultDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }

        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<SetResult>): List<Long> {
        val currentEntities = previousSetResultDao.getMany(models.map { it.id }).associateBy { it.id }
        val toUpsert =
            models.fastMap { setResult ->
                val current = currentEntities[setResult.id]
                setResult.toEntity().applyRemoteStorageMetadata(
                    remoteId = current?.remoteId,
                    remoteLastUpdated = current?.lastUpdated,
                    synced = false
                )
            }
        val ids = previousSetResultDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, id) ->
            if (id == -1L) entity else entity.copy(id = id)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun insert(model: SetResult): Long {
        val toInsert = model.toEntity()
        val id = previousSetResultDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun insertMany(models: List<SetResult>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val ids = previousSetResultDao.insertMany(toInsert)
        syncScheduler.scheduleSync()

        return ids
    }

    override suspend fun delete(model: SetResult): Int {
        val toDelete = model.toEntity()
        val count = previousSetResultDao.delete(toDelete)
        syncScheduler.scheduleSync()

        return count
    }

    override suspend fun deleteMany(models: List<SetResult>): Int {
        val toDelete = models.map { it.toEntity() }
        val count = previousSetResultDao.deleteMany(toDelete)
        syncScheduler.scheduleSync()

        return count
    }

    override suspend fun deleteAllForPreviousWorkout(
        workoutId: Long,
        currentMesocycle: Int,
        currentMicrocycle: Int,
        currentResultsToDeleteInstead: List<Long>,
    ) {
        val toDelete = previousSetResultDao.getAllForPreviousWorkout(
            workoutId = workoutId,
            currentMesocycle = currentMesocycle,
            currentMicrocycle = currentMicrocycle,
            currentResultsToDelete = currentResultsToDeleteInstead,
        )
        if (toDelete.isEmpty()) return

        previousSetResultDao.deleteMany(toDelete)
        syncScheduler.scheduleSync()
    }

    override suspend fun deleteAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int) {
        val toDelete = previousSetResultDao.getAllForWorkout(workoutId, mesoCycle, microCycle)
        if (toDelete.isEmpty()) return

        previousSetResultDao.deleteMany(toDelete)
        syncScheduler.scheduleSync()
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = previousSetResultDao.get(id) ?: return 0
        previousSetResultDao.delete(toDelete)
        syncScheduler.scheduleSync()

        return 1
    }
}