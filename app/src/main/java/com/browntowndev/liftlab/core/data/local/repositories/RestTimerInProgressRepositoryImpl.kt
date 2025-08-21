package com.browntowndev.liftlab.core.data.local.repositories

import android.util.Log
import com.browntowndev.liftlab.core.data.local.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.data.local.entities.RestTimerInProgressEntity
import com.browntowndev.liftlab.core.data.mapping.toDomainModel
import com.browntowndev.liftlab.core.domain.models.workoutLogging.RestTimerInProgress
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
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

    override suspend fun upsert(startTimeInMillis: Long, restTimeInMillis: Long) {
        restTimerInProgressDao.upsert(
            RestTimerInProgressEntity(
                timeStartedInMillis = startTimeInMillis,
                restTime = restTimeInMillis,
            )
        )
    }

    override suspend fun delete() {
        restTimerInProgressDao.delete()
    }
}