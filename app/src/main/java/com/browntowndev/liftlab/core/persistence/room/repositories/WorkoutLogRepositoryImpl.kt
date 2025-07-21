package com.browntowndev.liftlab.core.persistence.room.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.mapping.SetResultMappingExtensions.toSetResult
import com.browntowndev.liftlab.core.domain.mapping.WorkoutLogEntryMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.models.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository
import com.browntowndev.liftlab.core.persistence.room.dtos.FlattenedWorkoutLogEntryDto
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

class WorkoutLogRepositoryImpl(
    private val workoutLogEntryDao: WorkoutLogEntryDao,
    private val setLogEntryDao: SetLogEntryDao,
    private val firestoreSyncManager: FirestoreSyncManager,
) : WorkoutLogRepository {

    override fun getAll(): List<WorkoutLogEntry> =
        workoutLogEntryDao.getAll().fastMap { it.toDomainModel() }

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
}