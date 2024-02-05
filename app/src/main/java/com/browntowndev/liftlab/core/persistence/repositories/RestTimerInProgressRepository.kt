package com.browntowndev.liftlab.core.persistence.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.Utils
import com.browntowndev.liftlab.core.persistence.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.RestTimerInProgressDto
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class RestTimerInProgressRepository(private val restTimerInProgressDao: RestTimerInProgressDao): Repository {
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLive(): LiveData<RestTimerInProgressDto?> {
        return restTimerInProgressDao.getAsFlow()
            .flatMapLatest { restTimerInProgress ->
                flowOf(
                    if (restTimerInProgress != null) {
                        Log.d(Log.DEBUG.toString(), "rest timer in progress: ${restTimerInProgress.timeStartedInMillis}")
                        RestTimerInProgressDto(
                            id = restTimerInProgress.id,
                            timeStartedInMillis = restTimerInProgress.timeStartedInMillis,
                            restTime = restTimerInProgress.restTime,
                        )
                    } else null
                )
            }.asLiveData()
    }

    suspend fun insert(restTime: Long) {
        deleteAll()
        restTimerInProgressDao.insert(
            RestTimerInProgress(
                timeStartedInMillis = Utils.getCurrentDate().time,
                restTime = restTime,
            )
        )
    }

    suspend fun deleteAll() {
        restTimerInProgressDao.deleteAll()
    }
}