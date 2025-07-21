package com.browntowndev.liftlab.core.persistence.room.entities

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.mapping.SetLogEntryMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLogEntryMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.models.PersonalRecord
import com.browntowndev.liftlab.core.domain.models.SetLogEntry
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import com.browntowndev.liftlab.core.persistence.room.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.persistence.room.dtos.PersonalRecordDto

class SetLogEntryRepositoryImpl(
    private val setLogEntryDao: SetLogEntryDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): SetLogEntryRepository {

    override suspend fun insertFromPreviousSetResults(
        workoutLogEntryId: Long,
        workoutId: Long,
        mesocycle: Int,
        microcycle: Int,
        excludeFromCopy: List<Long>
    ) {
        setLogEntryDao.insertFromPreviousSetResults(
            workoutLogEntryId = workoutLogEntryId,
            workoutId = workoutId,
            mesocycle = mesocycle,
            microcycle = microcycle,
            excludeFromCopy = excludeFromCopy,
        )

        val insertedEntities = setLogEntryDao.getForWorkoutLogEntryMesoAndMicro(
            workoutLogEntryId = workoutLogEntryId,
            mesocycle = mesocycle,
            microcycle = microcycle,
        )

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION,
                roomEntityIds = insertedEntities.fastMap { it.id },
                SyncType.Upsert,
            )
        )
    }

    override suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecord> {
        return setLogEntryDao.getPersonalRecordsForLifts(liftIds).fastMap {
            PersonalRecord(
                liftId = it.liftId,
                personalRecord = it.personalRecord,
            )
        }
    }

    override suspend fun getAll(): List<SetLogEntry> =
        setLogEntryDao.getAll().fastMap { it.toDomainModel() }

    override suspend fun getById(id: Long): SetLogEntry? =
        setLogEntryDao.get(id)?.toDomainModel()

    override suspend fun getMany(ids: List<Long>): List<SetLogEntry> =
        setLogEntryDao.getMany(ids).fastMap { it.toDomainModel() }

    override suspend fun update(model: SetLogEntry) {
        TODO("Not yet implemented")
    }

    override suspend fun updateMany(models: List<SetLogEntry>) {
        TODO("Not yet implemented")
    }

    override suspend fun upsert(model: SetLogEntry): Long {
        val current = setLogEntryDao.get(model.id)
        val toUpsert = model.toEntity()
            .applyFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false
            )

        val id = setLogEntryDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )

        return if (id == -1L) toUpsert.id else id
    }

    override suspend fun upsertMany(models: List<SetLogEntry>): List<Long> {

        val currentEntries = setLogEntryDao.getMany(models.fastMap { it.id })
            .associateBy { it.id }
        val toUpsert = models.fastMap { setLogEntry ->
            val current = currentEntries[setLogEntry.id]
            setLogEntry.toEntity()
                .applyFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false
                )
        }
        val ids = setLogEntryDao.upsertMany(toUpsert)
        val entityIds = toUpsert.zip(ids).map {
            if (it.second == -1L) it.first else it.first.copy(id = it.second)
        }.fastMap { it.id }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION,
                roomEntityIds = entityIds,
                SyncType.Upsert,
            )
        )

        return entityIds
    }

    override suspend fun insert(model: SetLogEntry): Long {
        TODO("Not yet implemented")
    }

    override suspend fun insertMany(models: List<SetLogEntry>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(model: SetLogEntry): Int =
        deleteById(model.id)

    override suspend fun deleteMany(models: List<SetLogEntry>): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteById(id: Long): Int {
        val toDelete = setLogEntryDao.get(id) ?: return 0
        val deleteCount = setLogEntryDao.delete(toDelete)

        if (toDelete.firestoreId == null) return deleteCount
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION,
                roomEntityIds = listOf(toDelete.id),
                SyncType.Delete,
            )
        )

        return deleteCount
    }
}