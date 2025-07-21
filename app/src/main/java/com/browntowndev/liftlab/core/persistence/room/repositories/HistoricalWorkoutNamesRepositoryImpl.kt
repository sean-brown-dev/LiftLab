package com.browntowndev.liftlab.core.persistence.room.repositories

import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.models.HistoricalWorkoutName
import com.browntowndev.liftlab.core.domain.repositories.HistoricalWorkoutNamesRepository
import com.browntowndev.liftlab.core.persistence.room.dao.HistoricalWorkoutNamesDao
import com.browntowndev.liftlab.core.persistence.room.entities.HistoricalWorkoutNameEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry

class HistoricalWorkoutNamesRepositoryImpl(
    private val historicalWorkoutNamesDao: HistoricalWorkoutNamesDao,
    private val firestoreSyncManager: FirestoreSyncManager,
) : HistoricalWorkoutNamesRepository {
    override suspend fun getAll(): List<HistoricalWorkoutName> {
        TODO("Not yet implemented")
    }

    override suspend fun getById(id: Long): HistoricalWorkoutName? {
        TODO("Not yet implemented")
    }

    override suspend fun getMany(ids: List<Long>): List<HistoricalWorkoutName> {
        TODO("Not yet implemented")
    }

    override suspend fun update(model: HistoricalWorkoutName) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMany(models: List<HistoricalWorkoutName>) {
        TODO("Not yet implemented")
    }

    override suspend fun upsert(model: HistoricalWorkoutName): Long {
        TODO("Not yet implemented")
    }

    override suspend fun upsertMany(models: List<HistoricalWorkoutName>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun insert(model: HistoricalWorkoutName): Long {
        val toInsert = HistoricalWorkoutNameEntity(
            programId = model.programId,
            workoutId = model.workoutId,
            programName = model.programName,
            workoutName = model.workoutName,
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

    override suspend fun insertMany(models: List<HistoricalWorkoutName>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(model: HistoricalWorkoutName): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteMany(models: List<HistoricalWorkoutName>): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteById(id: Long): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getIdByProgramAndWorkoutId(programId: Long, workoutId: Long): Long? {
        return historicalWorkoutNamesDao.getByProgramAndWorkoutId(programId, workoutId)?.id
    }
}