package com.browntowndev.liftlab.core.data.local.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.local.dao.LiftsDao
import com.browntowndev.liftlab.core.data.local.entities.LiftEntity
import com.browntowndev.liftlab.core.data.local.entities.applyRemoteStorageMetadata
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.data.mapping.toEntity
import com.browntowndev.liftlab.core.sync.SyncScheduler
import com.browntowndev.liftlab.core.domain.models.metadata.LiftMetadata
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

class LiftsRepositoryImpl(
    private val liftsDao: LiftsDao,
    private val syncScheduler: SyncScheduler,
) : LiftsRepository {
    override suspend fun insert(model: Lift): Long {
        val toInsert = model.toEntity()
        val id = liftsDao.insert(toInsert)
        syncScheduler.scheduleSync()

        return id
    }

    override suspend fun update(model: Lift) {
        val current = liftsDao.get(model.id)
        val toUpdate = model.toEntity().applyRemoteStorageMetadata(
                remoteId = current?.remoteId,
                remoteLastUpdated = current?.remoteLastUpdated,
                synced = false,
            )
        liftsDao.update(toUpdate)
        syncScheduler.scheduleSync()
    }

    override suspend fun getAll(): List<Lift> {
        return liftsDao.getAll().fastMap { it.toDomainModel() }
    }

    override fun getAllFlow(): Flow<List<Lift>> {
        return liftsDao.getAllAsFlow().map { lifts ->
            lifts.fastMap { it.toDomainModel() }
        }
    }

    override fun getByIdFlow(liftId: Long): Flow<Lift?> {
        return liftsDao.getByIdFlow(liftId).map { it?.toDomainModel() }
    }

    override suspend fun updateRestTime(id: Long, enabled: Boolean, newRestTime: Duration?) {
        val current = liftsDao.get(id) ?: return
        val toUpdate = current.copy(restTimerEnabled = enabled, restTime = newRestTime)
            .applyRemoteStorageMetadata(
                remoteId = current.remoteId,
                remoteLastUpdated = current.remoteLastUpdated,
                synced = false,
            )
        updateWithoutRefetch(toUpdate)
    }

    override suspend fun updateIncrementOverride(id: Long, newIncrement: Float?) {
        val current = liftsDao.get(id) ?: return
        val toUpdate = current.copy(incrementOverride = newIncrement).applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    override suspend fun updateNote(id: Long, note: String?) {
        val current = liftsDao.get(id) ?: return
        val toUpdate = current.copy(note = note).applyRemoteStorageMetadata(
            remoteId = current.remoteId,
            remoteLastUpdated = current.remoteLastUpdated,
            synced = false,
        )
        updateWithoutRefetch(toUpdate)
    }

    override fun getManyMetadataFlow(ids: List<Long>): Flow<List<LiftMetadata>> {
        val liftEntities = liftsDao.getManyFlow(ids)
        return liftEntities.map {
            it.fastMap { liftEntity ->
                LiftMetadata(
                    id = liftEntity.id,
                    name = liftEntity.name,
                    note = liftEntity.note,
                    movementPattern = liftEntity.movementPattern,
                    volumeTypesBitmask = liftEntity.volumeTypesBitmask,
                    secondaryVolumeTypesBitmask = liftEntity.secondaryVolumeTypesBitmask,
                    restTime = liftEntity.restTime,
                    restTimerEnabled = liftEntity.restTimerEnabled,
                )
            }
        }
    }

    private suspend fun updateWithoutRefetch(liftEntity: LiftEntity) {
        liftsDao.update(liftEntity)
        syncScheduler.scheduleSync()
    }

    override suspend fun getById(id: Long): Lift? {
        return liftsDao.get(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<Lift> {
        return liftsDao.getMany(ids).fastMap { it.toDomainModel() }
    }

    override suspend fun updateMany(models: List<Lift>) {
        val existingById = liftsDao.getMany(models.map { it.id }).associateBy { it.id }
        val entities = models.fastMap {
            it.toEntity().applyRemoteStorageMetadata(
                remoteId = existingById[it.id]?.remoteId,
                remoteLastUpdated = existingById[it.id]?.remoteLastUpdated,
                synced = false,
            )
        }
        liftsDao.updateMany(entities)
        syncScheduler.scheduleSync()
    }

    override suspend fun upsert(model: Lift): Long {
        val current = liftsDao.get(model.id)
        val toUpsert = model.toEntity().applyRemoteStorageMetadata(
            remoteId = current?.remoteId,
            remoteLastUpdated = current?.remoteLastUpdated,
            synced = false,
        )
        val id = liftsDao.upsert(toUpsert)
        syncScheduler.scheduleSync()

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<Lift>): List<Long> {
        val existingById = liftsDao.getMany(models.map { it.id }).associateBy { it.id }
        val entities = models.map {
            it.toEntity().applyRemoteStorageMetadata(
                remoteId = existingById[it.id]?.remoteId,
                remoteLastUpdated = existingById[it.id]?.remoteLastUpdated,
                synced = false,
            )
        }
        val upsertResultIds = liftsDao.upsertMany(entities)
        val entityIds = entities.zip(upsertResultIds).fastMap { (entity, id) ->
            if (id == -1L) entity else entity.copy(id = id)
        }.fastMap { it.id }

        syncScheduler.scheduleSync()

        return entityIds
    }

    override suspend fun insertMany(models: List<Lift>): List<Long> {
        val entities = models.map { it.toEntity() }
        val ids = liftsDao.insertMany(entities)
        syncScheduler.scheduleSync()

        return ids
    }

    override suspend fun delete(model: Lift): Int {
        val count = liftsDao.softDelete(model.id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteMany(models: List<Lift>): Int {
        val ids = models.map { it.id }
        if (ids.isEmpty()) return 0
        val count = liftsDao.softDeleteMany(ids)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }

    override suspend fun deleteById(id: Long): Int {
        val count = liftsDao.softDelete(id)
        if (count > 0) {
            syncScheduler.scheduleSync()
        }
        return count
    }
}