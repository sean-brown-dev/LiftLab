package com.browntowndev.liftlab.core.domain.repositories.standard

import android.util.Log
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.persistence.room.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.domain.models.RestTimerInProgress
import com.browntowndev.liftlab.core.persistence.entities.room.RestTimerInProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RestTimerInProgressRepository(
    private val restTimerInProgressDao: RestTimerInProgressDao,
): Repository {
    suspend fun get(): RestTimerInProgress? {
        val restTimerInProgress = restTimerInProgressDao.get()

        return if (restTimerInProgress != null) {
            Log.d(Log.DEBUG.toString(), "rest timer in progress: ${restTimerInProgress.timeStartedInMillis}")
            RestTimerInProgress(
                id = restTimerInProgress.id,
                timeStartedInMillis = restTimerInProgress.timeStartedInMillis,
                restTime = restTimerInProgress.restTime,
            )
        } else null
    }

    fun getFlow(): Flow<RestTimerInProgress?> {
        return restTimerInProgressDao.getAsFlow()
            .map { restTimerInProgress ->
                restTimerInProgress?.let {
                    Log.d(Log.DEBUG.toString(), "rest timer in progress: ${it.timeStartedInMillis}")
                    RestTimerInProgress(
                        id = it.id,
                        timeStartedInMillis = it.timeStartedInMillis,
                        restTime = it.restTime,
                    )
                }
            }
    }

    suspend fun insert(restTime: Long) {
        deleteAll()
        val toInsert = RestTimerInProgressEntity(
                timeStartedInMillis = getCurrentDate().time,
                restTime = restTime,
            )
        restTimerInProgressDao.insert(toInsert)
    }

    suspend fun deleteAll() {
        restTimerInProgressDao.deleteAll()
    }
}