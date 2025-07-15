package com.browntowndev.liftlab.core.persistence.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.Utils.General.Companion.getCurrentDate
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.RestTimerInProgressDao
import com.browntowndev.liftlab.core.persistence.dtos.RestTimerInProgressDto
import com.browntowndev.liftlab.core.persistence.entities.RestTimerInProgress
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class RestTimerInProgressRepository(
    private val restTimerInProgressDao: RestTimerInProgressDao,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val syncScope: CoroutineScope,
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
        val toInsert = RestTimerInProgress(
                timeStartedInMillis = getCurrentDate().time,
                restTime = restTime,
            )
        val id = restTimerInProgressDao.insert(toInsert)
        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.REST_TIMER_IN_PROGRESS_COLLECTION,
                entity = toInsert.copy(id = id).toFirestoreDto(),
                onSynced = {
                    restTimerInProgressDao.update(it.toEntity())
                }
            )
        }
    }

    suspend fun deleteAll() {
        val toDelete = restTimerInProgressDao.getAll()
        if (toDelete.isEmpty()) return

        restTimerInProgressDao.deleteMany(toDelete)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.deleteMany(
                collectionName = FirestoreConstants.REST_TIMER_IN_PROGRESS_COLLECTION,
                firestoreIds = toDelete.mapNotNull { it.firestoreId },
            )
        }
    }
}