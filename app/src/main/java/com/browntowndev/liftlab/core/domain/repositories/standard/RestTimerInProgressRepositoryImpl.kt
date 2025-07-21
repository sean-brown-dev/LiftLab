package com.browntowndev.liftlab.core.domain.repositories.standard

import android.util.Log
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.domain.mapping.RestTimerInProgressMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.mapping.RestTimerInProgressMappingExtensions.toEntity
import com.browntowndev.liftlab.core.persistence.room.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.domain.models.RestTimerInProgress
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.room.RestTimerInProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RestTimerInProgressRepositoryImpl(
    private val restTimerInProgressDao: RestTimerInProgressDao,
): RestTimerInProgressRepository {
    override suspend fun get(): RestTimerInProgress? {
        val restTimerInProgress = restTimerInProgressDao.get()

        return if (restTimerInProgress != null) {
            Log.d(Log.DEBUG.toString(), "rest timer in progress: ${restTimerInProgress.timeStartedInMillis}")
            restTimerInProgress.toDomainModel()
        } else null
    }

    override fun getFlow(): Flow<RestTimerInProgress?> {
        return restTimerInProgressDao.getAsFlow()
            .map { restTimerInProgress ->
                restTimerInProgress?.let {
                    Log.d(Log.DEBUG.toString(), "rest timer in progress: ${it.timeStartedInMillis}")
                    it.toDomainModel()
                }
            }
    }

    override suspend fun insert(restTime: Long) {
        deleteAll()
        val toInsert = RestTimerInProgressEntity(
                timeStartedInMillis = getCurrentDate().time,
                restTime = restTime,
            )
        restTimerInProgressDao.insert(toInsert)
    }

    override suspend fun deleteAll() {
        restTimerInProgressDao.deleteAll()
    }

    override suspend fun getAll(): List<RestTimerInProgress> =
        restTimerInProgressDao.getAll().fastMap { it.toDomainModel() }

    override suspend fun getById(id: Long): RestTimerInProgress? =
        restTimerInProgressDao.getById(id)?.toDomainModel()

    override suspend fun getMany(ids: List<Long>): List<RestTimerInProgress> =
        restTimerInProgressDao.getMany(ids).fastMap { it.toDomainModel() }

    override suspend fun update(model: RestTimerInProgress) =
        restTimerInProgressDao.update(model.toEntity())

    override suspend fun updateMany(models: List<RestTimerInProgress>) =
        restTimerInProgressDao.updateMany(models.map { it.toEntity() })

    override suspend fun upsert(model: RestTimerInProgress): Long =
        restTimerInProgressDao.upsert(model.toEntity())

    override suspend fun upsertMany(models: List<RestTimerInProgress>): List<Long> =
        restTimerInProgressDao.upsertMany(models.map { it.toEntity() })

    override suspend fun insert(model: RestTimerInProgress): Long =
        restTimerInProgressDao.insert(model.toEntity())

    override suspend fun insertMany(models: List<RestTimerInProgress>): List<Long> =
        restTimerInProgressDao.insertMany(models.map { it.toEntity() })

    override suspend fun delete(model: RestTimerInProgress): Int =
        restTimerInProgressDao.delete(model.toEntity())

    override suspend fun deleteMany(models: List<RestTimerInProgress>): Int =
        restTimerInProgressDao.deleteMany(models.map { it.toEntity() })

    override suspend fun deleteById(id: Long): Int {
        val toDelete = restTimerInProgressDao.getById(id) ?: return 0
        return restTimerInProgressDao.delete(toDelete)
    }

}