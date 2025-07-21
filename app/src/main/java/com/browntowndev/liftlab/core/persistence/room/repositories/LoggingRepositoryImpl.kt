package com.browntowndev.liftlab.core.persistence.room.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.mapping.SetResultMappingExtensions.toSetResult
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLogEntryMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLogEntryMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.models.SetLogEntry
import com.browntowndev.liftlab.core.domain.models.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.LoggingRepository
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.room.dtos.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.room.dtos.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.room.entities.WorkoutLogEntryEntity
import com.browntowndev.liftlab.core.persistence.firestore.sync.BatchSyncQueueEntry
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import com.browntowndev.liftlab.core.persistence.room.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutLogEntryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.UUID

class LoggingRepositoryImpl(
    private val workoutLogEntryDao: WorkoutLogEntryDao,
    private val setLogEntryDao: SetLogEntryDao,
    private val firestoreSyncManager: FirestoreSyncManager,
) : LoggingRepository {

    override fun getAllFlow(): Flow<List<WorkoutLogEntry>> {
        return workoutLogEntryDao.getAllFlattened().map {
            it.toDomainModel()
        }
    }

    override suspend fun get(workoutLogEntryId: Long): WorkoutLogEntry? {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> =
            workoutLogEntryDao.getFlattened(workoutLogEntryId = workoutLogEntryId)
        return flattenedLogEntries.toDomainModel().firstOrNull()
    }

    override suspend fun getWorkoutLogsForLift(liftId: Long): List<WorkoutLogEntry> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> =
            workoutLogEntryDao.getLogsByLiftId(liftId)
        return flattenedLogEntries.toDomainModel()
    }

    private suspend fun getMostRecentLogsForLiftIds(
        liftIds: List<Long>,
        includeDeload: Boolean
    ): List<WorkoutLogEntry> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> = if (includeDeload) {
            workoutLogEntryDao.getMostRecentLogsForLiftIds(liftIds)
        } else {
            workoutLogEntryDao.getMostRecentLogsForLiftIdsExcludingDeloads(liftIds)
        }

        return flattenedLogEntries.toDomainModel()
    }

    override suspend fun getMostRecentSetResultsForLiftIds(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        includeDeload: Boolean,
    ): List<SetResult> {
        return getMostRecentLogsForLiftIds(liftIds, includeDeload)
            .flatMap { workoutLog ->
                workoutLog.setResults.fastMap { setLogEntry ->
                    setLogEntry.toSetResult(
                        workoutId = workoutLog.workoutId,
                        isLinearProgression = linearProgressionLiftIds.contains(
                            setLogEntry.liftId
                        )
                    )
                }
            }
    }

    override suspend fun getMostRecentSetResultsForLiftIdsPriorToDate(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        date: Date,
    ): List<SetResult> {
        return workoutLogEntryDao.getMostRecentLogsForLiftIdsPriorToDate(liftIds, date)
            .toDomainModel()
            .flatMap { workoutLog ->
                workoutLog.setResults.fastMap { setLogEntry ->
                    setLogEntry.toSetResult(
                        workoutId = workoutLog.workoutId,
                        isLinearProgression = linearProgressionLiftIds.contains(
                            setLogEntry.liftId
                        )
                    )
                }
            }
    }

    override suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecordDto> {
        return setLogEntryDao.getPersonalRecordsForLifts(liftIds)
    }

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

    override suspend fun insertWorkoutLogEntry(
        historicalWorkoutNameId: Long,
        programDeloadWeek: Int,
        programWorkoutCount: Int,
        mesoCycle: Int,
        microCycle: Int,
        microcyclePosition: Int,
        date: Date,
        durationInMillis: Long,
    ): Long {
        val toInsert =
            WorkoutLogEntryEntity(
                historicalWorkoutNameId = historicalWorkoutNameId,
                programDeloadWeek = programDeloadWeek,
                programWorkoutCount = programWorkoutCount,
                mesocycle = mesoCycle,
                microcycle = microCycle,
                microcyclePosition = microcyclePosition,
                date = date,
                durationInMillis = durationInMillis,
            )
        val id = workoutLogEntryDao.insert(toInsert)

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUT_LOG_ENTRIES_COLLECTION,
                roomEntityIds = listOf(id),
                SyncType.Upsert,
            )
        )

        return id
    }

    override suspend fun deleteWorkoutLogEntry(workoutLogEntryId: Long) {
        val setLogEntriesToDelete = setLogEntryDao.getForWorkoutLogEntry(workoutLogEntryId)
        if (setLogEntriesToDelete.isEmpty()) return

        setLogEntryDao.deleteMany(setLogEntriesToDelete)

        val workoutLogEntryToDelete = workoutLogEntryDao.get(workoutLogEntryId)
            ?: throw Exception("WorkoutEntity log entry not found")
        workoutLogEntryDao.delete(workoutLogEntryToDelete)

        val batchesToSync = buildList {
            setLogEntriesToDelete
                .fastMapNotNull { it.firestoreId?.let { _ -> it.id } }
                .takeIf { it.isNotEmpty() }
                ?.let { ids ->
                    add(
                        SyncQueueEntry(
                            collectionName = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION,
                            roomEntityIds = ids,
                            syncType = SyncType.Delete,
                        )
                    )
                }

            workoutLogEntryToDelete.firestoreId?.let {
                add(
                    SyncQueueEntry(
                        collectionName = FirestoreConstants.WORKOUT_LOG_ENTRIES_COLLECTION,
                        roomEntityIds = listOf(workoutLogEntryToDelete.id),
                        syncType = SyncType.Delete,
                    )
                )
            }
        }

        if (batchesToSync.isEmpty()) return
        firestoreSyncManager.enqueueBatchSyncRequest(
            BatchSyncQueueEntry(
                id = UUID.randomUUID().toString(),
                batch = batchesToSync
            )
        )
    }

    override suspend fun deleteSetLogEntryById(id: Long) {
        val toDelete = setLogEntryDao.get(id) ?: return
        setLogEntryDao.delete(toDelete)

        if (toDelete.firestoreId == null) return
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION,
                roomEntityIds = listOf(toDelete.id),
                SyncType.Delete,
            )
        )
    }

    override suspend fun upsert(workoutLogEntryId: Long, setLogEntry: SetLogEntry): Long {
        val current = setLogEntryDao.get(setLogEntry.id)
        val toUpsert = setLogEntry.toEntity(workoutLogEntryId)
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

        return id
    }

    override suspend fun upsertMany(
        workoutLogEntryId: Long,
        setLogEntries: List<SetLogEntry>
    ): List<Long> {
        val currentEntries = setLogEntryDao.getMany(setLogEntries.fastMap { it.id })
            .associateBy { it.id }
        val toUpsert = setLogEntries.fastMap { setLogEntry ->
            val current = currentEntries[setLogEntry.id]
            setLogEntry.toEntity(workoutLogEntryId)
                .applyFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false
                )
        }
        val ids = setLogEntryDao.upsertMany(toUpsert)
        toUpsert.zip(ids).map {
            if (it.second == -1L) it.first else it.first.copy(id = it.second)
        }

        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.SET_LOG_ENTRIES_COLLECTION,
                roomEntityIds = toUpsert.fastMap { it.id },
                SyncType.Upsert,
            )
        )

        return ids
    }
}