package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.sync.SyncQueueEntry

class HistoricalWorkoutNamesRepository(
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {

    suspend fun insert(programId: Long, workoutId: Long, programName: String, workoutName: String): Long {
        val toInsert = HistoricalWorkoutName(
            programId = programId,
            workoutId = workoutId,
            programName = programName,
            workoutName = workoutName,
        )
        val id = historicalWorkoutNamesDao.insert(toInsert)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )

        return id
    }

    suspend fun getIdByProgramAndWorkoutId(programId: Long, workoutId: Long): Long? {
        return historicalWorkoutNamesDao.getByProgramAndWorkoutId(programId, workoutId)?.id
    }
}