package com.browntowndev.liftlab.core.persistence.repositories

import android.util.Log
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.persistence.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.RestTimerInProgressDto
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RestTimerInProgressRepository(
    private val restTimerInProgressDao: RestTimerInProgressDao,
): Repository {
    suspend fun get(): RestTimerInProgressDto? {
        val restTimerInProgress = restTimerInProgressDao.get()

        return if (restTimerInProgress != null) {
            Log.d(Log.DEBUG.toString(), "rest timer in progress: ${restTimerInProgress.timeStartedInMillis}")
            RestTimerInProgressDto(
                id = restTimerInProgress.id,
                timeStartedInMillis = restTimerInProgress.timeStartedInMillis,
                restTime = restTimerInProgress.restTime,
            )
        } else null
    }

    fun getFlow(): Flow<RestTimerInProgressDto?> {
        return restTimerInProgressDao.getAsFlow()
            .map { restTimerInProgress ->
                restTimerInProgress?.let {
                    Log.d(Log.DEBUG.toString(), "rest timer in progress: ${it.timeStartedInMillis}")
                    RestTimerInProgressDto(
                        id = it.id,
                        timeStartedInMillis = it.timeStartedInMillis,
                        restTime = it.restTime,
                    )
                }
            }
    }

    suspend fun insert(restTime: Long) {
        deleteAll()
        val toInsert = RestTimerInProgress(
                timeStartedInMillis = getCurrentDate().time,
                restTime = restTime,
            )
        restTimerInProgressDao.insert(toInsert)
    }

    suspend fun deleteAll() {
        restTimerInProgressDao.deleteAll()
    }
}