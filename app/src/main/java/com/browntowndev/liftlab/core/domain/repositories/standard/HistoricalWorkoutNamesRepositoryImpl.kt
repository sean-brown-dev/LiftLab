package com.browntowndev.liftlab.core.domain.repositories.standard

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.persistence.room.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.entities.room.HistoricalWorkoutNameEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry

class HistoricalWorkoutNamesRepositoryImpl(
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {

    suspend fun insert(programId: Long, workoutId: Long, programName: String, workoutName: String): Long {
        val toInsert = HistoricalWorkoutNameEntity(
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