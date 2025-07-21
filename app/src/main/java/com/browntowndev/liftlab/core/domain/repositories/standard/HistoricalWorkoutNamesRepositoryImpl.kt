package com.browntowndev.liftlab.core.domain.repositories.standard

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.models.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.room.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.entities.room.HistoricalWorkoutNameEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry

class HistoricalWorkoutNamesRepositoryImpl(
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao,
    private val firestoreSyncManager: FirestoreSyncManager,
) : HistoricalWorkoutNamesRepository {

    override suspend fun insert(historicalWorkoutName: HistoricalWorkoutName): Long {
        val toInsert = HistoricalWorkoutNameEntity(
            programId = historicalWorkoutName.programId,
            workoutId = historicalWorkoutName.workoutId,
            programName = historicalWorkoutName.programName,
            workoutName = historicalWorkoutName.workoutName,
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

    override suspend fun getIdByProgramAndWorkoutId(programId: Long, workoutId: Long): Long? {
        return historicalWorkoutNamesDao.getByProgramAndWorkoutId(programId, workoutId)?.id
    }
}