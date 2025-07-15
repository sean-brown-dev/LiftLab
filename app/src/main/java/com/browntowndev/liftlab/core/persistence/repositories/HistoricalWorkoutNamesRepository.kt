package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.entities.HistoricalWorkoutName
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager

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
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.HISTORICAL_WORKOUT_NAMES_COLLECTION,
            entity = toInsert.toFirebaseDto(),
            onSynced = {
                historicalWorkoutNamesDao.update(it.toEntity())
            }
        )

        return id
    }

    suspend fun getIdByProgramAndWorkoutId(programId: Long, workoutId: Long): Long? {
        return historicalWorkoutNamesDao.getByProgramAndWorkoutId(programId, workoutId)?.id
    }
}