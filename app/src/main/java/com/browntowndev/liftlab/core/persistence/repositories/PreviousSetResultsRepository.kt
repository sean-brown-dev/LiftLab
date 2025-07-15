package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.PreviousSetResultDao
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.dtos.queryable.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager

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
            .copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false
            )
        val id = previousSetResultDao.upsert(toUpsert)
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.PREVIOUS_SET_RESULTS_COLLECTION,
            entity = toUpsert.toFirebaseDto().copy(id = id),
            onSynced = {
                previousSetResultDao.update(it.toEntity())
            }
        )

        return id
    }

    suspend fun upsertMany(setResults: List<SetResult>): List<Long> {
        val currentEntities = previousSetResultDao.getMany(setResults.map { it.id }).associateBy { it.id }
        var toUpsert =
            setResults.fastMap { setResult ->
                val current = currentEntities[setResult.id]
                setResultsMapper.map(setResult).copyWithFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false
                )
            }
        val ids = previousSetResultDao.upsertMany(toUpsert)
        toUpsert = toUpsert.zip(ids).map { (entity, id) ->
            if (id == -1L) entity else entity.copy(id = id)
        }
        firestoreSyncManager.syncMany(
            collectionName = FirebaseConstants.PREVIOUS_SET_RESULTS_COLLECTION,
            entities = toUpsert.map { it.toFirebaseDto() },
            onSynced = { firestoreEntities ->
                previousSetResultDao.updateMany(firestoreEntities.map { it.toEntity() })
            }
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
        previousSetResultDao.deleteMany(toDelete)
        firestoreSyncManager.deleteMany(
            collectionName = FirebaseConstants.PREVIOUS_SET_RESULTS_COLLECTION,
            firestoreIds = toDelete.mapNotNull { it.firestoreId },
        )
    }

    suspend fun deleteAllForWorkout(workoutId: Long, mesoCycle: Int, microCycle: Int) {
        val toDelete = previousSetResultDao.getAllForWorkout(workoutId, mesoCycle, microCycle)
        previousSetResultDao.deleteMany(toDelete)
        firestoreSyncManager.deleteMany(
            collectionName = FirebaseConstants.PREVIOUS_SET_RESULTS_COLLECTION,
            firestoreIds = toDelete.mapNotNull { it.firestoreId },
        )
    }

    suspend fun deleteById(id: Long) {
        previousSetResultDao.get(id)?.let { toDelete ->
            previousSetResultDao.delete(toDelete)

            if (toDelete.firestoreId != null) {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirebaseConstants.PREVIOUS_SET_RESULTS_COLLECTION,
                    firestoreId = toDelete.firestoreId!!,
                )
            }
        }
    }
}
