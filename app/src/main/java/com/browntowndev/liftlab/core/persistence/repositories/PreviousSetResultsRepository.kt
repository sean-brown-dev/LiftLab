package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.dtos.queryable.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.sync.SyncQueueEntry
import kotlinx.coroutines.CoroutineScope

class PreviousSetResultsRepository(
    private val previousSetResultDao: PreviousSetResultDao,
    private val setResultsMapper: SetResultMapper,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {
    suspend fun getByWorkoutIdExcludingGivenMesoAndMicro(workoutId: Long, mesoCycle: Int, microCycle: Int): List<SetResult> {
        return previousSetResultDao.getByWorkoutIdExcludingGivenMesoAndMicro(workoutId, mesoCycle, microCycle).map {
            setResultsMapper.map(it)
        }
    }

    suspend fun getForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int): List<SetResult> {
        return previousSetResultDao.getForWorkout(workoutId, mesoCycle, microCycle).map {
            setResultsMapper.map(it)
        }
    }

    suspend fun getPersonalRecordsForLiftsExcludingWorkout(
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

    suspend fun upsert(setResult: SetResult): Long {
        val current = previousSetResultDao.get(setResult.id)
        val toUpsert = setResultsMapper.map(setResult)
            .applyFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false
            )
        val id = previousSetResultDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )

        return id
    }

    suspend fun upsertMany(setResults: List<SetResult>): List<Long> {
        val currentEntities = previousSetResultDao.getMany(setResults.map { it.id }).associateBy { it.id }
        var toUpsert =
            setResults.fastMap { setResult ->
                val current = currentEntities[setResult.id]
                setResultsMapper.map(setResult).applyFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false
                )
            }
        val ids = previousSetResultDao.upsertMany(toUpsert)
        toUpsert = toUpsert.zip(ids).map { (entity, id) ->
            if (id == -1L) entity else entity.copy(id = id)
        }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION,
                roomEntityIds = toUpsert.fastMap { it.id },
                SyncType.Upsert,
            )
        )

        return ids
    }

    suspend fun deleteAllForPreviousWorkout(
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

        toDelete.fastMapNotNull { it.firestoreId?.let { _ -> it.id } }
            .takeIf { it.isNotEmpty() }
            ?.let { ids ->
                firestoreSyncManager.enqueueSyncRequest(
                    SyncQueueEntry(
                        collectionName = FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION,
                        roomEntityIds = ids,
                        SyncType.Delete,
                    )
                )
            }
    }

    suspend fun deleteAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int) {
        val toDelete = previousSetResultDao.getAllForWorkout(workoutId, mesoCycle, microCycle)
        if (toDelete.isEmpty()) return

        previousSetResultDao.deleteMany(toDelete)

        toDelete.fastMapNotNull { it.firestoreId?.let { _ -> it.id } }
            .takeIf { it.isNotEmpty() }
            ?.let { ids ->
                firestoreSyncManager.enqueueSyncRequest(
                    SyncQueueEntry(
                        collectionName = FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION,
                        roomEntityIds = ids,
                        SyncType.Delete,
                    )
                )
            }
    }

    suspend fun deleteById(id: Long) {
        val toDelete = previousSetResultDao.get(id) ?: return
        previousSetResultDao.delete(toDelete)

        if (toDelete.firestoreId != null) {
            firestoreSyncManager.enqueueSyncRequest(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.PREVIOUS_SET_RESULTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }
    }
}
