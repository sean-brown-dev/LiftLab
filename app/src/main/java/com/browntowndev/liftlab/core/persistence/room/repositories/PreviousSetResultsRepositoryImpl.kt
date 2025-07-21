package com.browntowndev.liftlab.core.persistence.room.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.room.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.room.dtos.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.domain.mapping.SetResultMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.mapping.SetResultMappingExtensions.toSetResult
import com.browntowndev.liftlab.core.domain.repositories.PreviousSetResultsRepository
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreviousSetResultsRepositoryImpl(
    private val previousSetResultDao: PreviousSetResultDao,
    private val firestoreSyncManager: FirestoreSyncManager,
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
        TODO("Not yet implemented")
    }

    override suspend fun getById(id: Long): SetResult? {
        TODO("Not yet implemented")
    }

    override suspend fun getMany(ids: List<Long>): List<SetResult> {
        TODO("Not yet implemented")
    }

    override suspend fun update(model: SetResult) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMany(models: List<SetResult>) {
        TODO("Not yet implemented")
    }

    override suspend fun upsert(model: SetResult): Long {
        val current = previousSetResultDao.get(model.id)
        val toUpsert = model.toEntity()
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

    override suspend fun upsertMany(models: List<SetResult>): List<Long> {
        val currentEntities = previousSetResultDao.getMany(models.map { it.id }).associateBy { it.id }
        var toUpsert =
            models.fastMap { setResult ->
                val current = currentEntities[setResult.id]
                setResult.toEntity().applyFirestoreMetadata(
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

    override suspend fun insert(model: SetResult): Long {
        TODO("Not yet implemented")
    }

    override suspend fun insertMany(models: List<SetResult>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(model: SetResult): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMany(models: List<SetResult>): Int {
        TODO("Not yet implemented")
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

    override suspend fun deleteAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int) {
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

    override suspend fun deleteById(id: Long): Int {
        val toDelete = previousSetResultDao.get(id) ?: return 0
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
        return 1
    }
}