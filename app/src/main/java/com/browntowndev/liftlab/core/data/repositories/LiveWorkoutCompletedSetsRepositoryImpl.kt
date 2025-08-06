package com.browntowndev.liftlab.core.data.repositories

import android.util.Log
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.LiveWorkoutCompletedSetsDao
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.SetResultMappingExtensions.toEntity
import com.browntowndev.liftlab.core.data.mapping.SetResultMappingExtensions.toSetResult
import com.browntowndev.liftlab.core.data.remote.SyncScheduler
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LiveWorkoutCompletedSetsRepositoryImpl(
    private val liveWorkoutCompletedSetsDao: LiveWorkoutCompletedSetsDao,
    private val syncScheduler: SyncScheduler,
): LiveWorkoutCompletedSetsRepository {
    override suspend fun getAll(): List<SetResult> {
        return liveWorkoutCompletedSetsDao.getAll().map { it.toSetResult() }
    }

    override fun getAllFlow(): Flow<List<SetResult>> {
        return liveWorkoutCompletedSetsDao.getAllFlow().map { setResults -> setResults.fastMap { it.toSetResult() } }
    }

    override suspend fun getById(id: Long): SetResult? {
        return liveWorkoutCompletedSetsDao.get(id)?.toSetResult()
    }

    override suspend fun getMany(ids: List<Long>): List<SetResult> {
        return liveWorkoutCompletedSetsDao.getMany(ids).map { it.toSetResult() }
    }

    override suspend fun update(model: SetResult) {
        val toUpdate = model.toEntity()
        liveWorkoutCompletedSetsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun updateMany(models: List<SetResult>) {
        val toUpdate = models.map { it.toEntity() }
        liveWorkoutCompletedSetsDao.updateMany(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: SetResult): Long {
        val current = liveWorkoutCompletedSetsDao.get(model.id)
        val toUpsert = model.toEntity()
            .applyRemoteStorageMetadata(
                remoteId = current?.remoteId,
                remoteLastUpdated = current?.remoteLastUpdated,
                synced = false
            )
        val id = liveWorkoutCompletedSetsDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }

        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<SetResult>): List<Long> {
        if (models.isEmpty()) return emptyList()
        val currentEntities = liveWorkoutCompletedSetsDao.getMany(models.map { it.id }).associateBy { it.id }
        val toUpsert =
            models.fastMap { setResult ->
                val current = currentEntities[setResult.id]
                setResult.toEntity().applyRemoteStorageMetadata(
                    remoteId = current?.remoteId,
                    remoteLastUpdated = current?.remoteLastUpdated,
                    synced = false
                )
            }
        val ids = liveWorkoutCompletedSetsDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map { (entity, id) ->
            if (id == -1L) entity else entity.copy(id = id)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun insert(model: SetResult): Long {
        val toInsert = model.toEntity()
        val id = liveWorkoutCompletedSetsDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun insertMany(models: List<SetResult>): List<Long> {
        val toInsert = models.map { it.toEntity() }
        val ids = liveWorkoutCompletedSetsDao.insertMany(toInsert)
        syncScheduler.scheduleSync()

        return ids
    }

    override suspend fun deleteAll() =
        liveWorkoutCompletedSetsDao.softDeleteAll()

    override suspend fun delete(model: SetResult): Int {
        val count = liveWorkoutCompletedSetsDao.softDelete(model.id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteMany(models: List<SetResult>): Int {
        val ids = models.map { it.id }
        if (ids.isEmpty()) return 0
        val count = liveWorkoutCompletedSetsDao.softDeleteMany(ids)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        Log.d("PreviousSetResultsRepositoryImpl", "deleteById: $id")
        val count = liveWorkoutCompletedSetsDao.softDelete(id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }
}