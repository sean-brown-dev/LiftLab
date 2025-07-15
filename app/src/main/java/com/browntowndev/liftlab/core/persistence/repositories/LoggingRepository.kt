package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.common.FirebaseConstants
import com.browntowndev.liftlab.core.persistence.dao.SetLogEntryDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutLogEntryDao
import com.browntowndev.liftlab.core.persistence.dtos.SetLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.SetResult
import com.browntowndev.liftlab.core.persistence.dtos.queryable.FlattenedWorkoutLogEntryDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.PersonalRecordDto
import com.browntowndev.liftlab.core.persistence.entities.WorkoutLogEntry
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirebaseDto
import com.browntowndev.liftlab.core.persistence.mapping.SetResultMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutLogEntryMapper
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Date

class LoggingRepository(
    private val workoutLogEntryDao: WorkoutLogEntryDao,
    private val setLogEntryDao: SetLogEntryDao,
    private val workoutLogEntryMapper: WorkoutLogEntryMapper,
    private val setResultMapper: SetResultMapper,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAll(): LiveData<List<WorkoutLogEntryDto>> {
        return workoutLogEntryDao.getAllFlattened().flatMapLatest {
            flowOf(workoutLogEntryMapper.map(it))
        }.asLiveData()
    }

    suspend fun get(workoutLogEntryId: Long): WorkoutLogEntryDto? {
        val log: List<FlattenedWorkoutLogEntryDto> = workoutLogEntryDao.getFlattened(workoutLogEntryId = workoutLogEntryId)
        return workoutLogEntryMapper.map(log).singleOrNull()
    }

    suspend fun getWorkoutLogsForLift(liftId: Long): List<WorkoutLogEntryDto> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> = workoutLogEntryDao.getLogsByLiftId(liftId)
        return workoutLogEntryMapper.map(flattenedLogEntries)
    }

    private suspend fun getMostRecentLogsForLiftIds(liftIds: List<Long>, includeDeload: Boolean): List<WorkoutLogEntryDto> {
        val flattenedLogEntries: List<FlattenedWorkoutLogEntryDto> = if(includeDeload) {
            workoutLogEntryDao.getMostRecentLogsForLiftIds(liftIds)
        } else {
            workoutLogEntryDao.getMostRecentLogsForLiftIdsExcludingDeloads(liftIds)
        }

        return workoutLogEntryMapper.map(flattenedLogEntries)
    }

    suspend fun getMostRecentSetResultsForLiftIds(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        includeDeload: Boolean,
    ): List<SetResult> {
        return getMostRecentLogsForLiftIds(liftIds, includeDeload)
            .flatMap { workoutLog ->
                workoutLog.setResults.fastMap { setLogEntry ->
                    setResultMapper.map(
                        from = setLogEntry,
                        workoutId = workoutLog.workoutId,
                        isLinearProgression = linearProgressionLiftIds.contains(
                            setLogEntry.liftId
                        )
                    )
                }
            }
    }

    suspend fun getMostRecentSetResultsForLiftIdsPriorToDate(
        liftIds: List<Long>,
        linearProgressionLiftIds: Set<Long>,
        date: Date,
    ): List<SetResult> {
        return workoutLogEntryMapper.map(
            workoutLogEntryDao.getMostRecentLogsForLiftIdsPriorToDate(liftIds, date)
        ).flatMap { workoutLog ->
            workoutLog.setResults.fastMap { setLogEntry ->
                setResultMapper.map(
                    from = setLogEntry,
                    workoutId = workoutLog.workoutId,
                    isLinearProgression = linearProgressionLiftIds.contains(
                        setLogEntry.liftId
                    )
                )
            }
        }
    }

    suspend fun getPersonalRecordsForLifts(liftIds: List<Long>): List<PersonalRecordDto> {
        return setLogEntryDao.getPersonalRecordsForLifts(liftIds)
    }

    suspend fun insertFromPreviousSetResults(
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
        firestoreSyncManager.syncMany(
            collectionName = FirebaseConstants.SET_LOG_ENTRIES_COLLECTION,
            entities = insertedEntities.map { it.toFirebaseDto() },
            onSynced = { firestoreEntities ->
                setLogEntryDao.updateMany(firestoreEntities.map { it.toEntity() })
            }
        )
    }

    suspend fun insertWorkoutLogEntry(
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
            WorkoutLogEntry(
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
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.WORKOUT_LOG_ENTRIES_COLLECTION,
            entity = toInsert.toFirebaseDto().copy(id = id),
            onSynced = { firestoreEntity ->
                workoutLogEntryDao.update(firestoreEntity.toEntity())
            }
        )

        return id
    }

    suspend fun deleteWorkoutLogEntry(workoutLogEntryId: Long) {
        val setLogEntriesToDelete = setLogEntryDao.getForWorkoutLogEntry(workoutLogEntryId)
        setLogEntryDao.deleteMany(setLogEntriesToDelete)
        firestoreSyncManager.deleteMany(
            collectionName = FirebaseConstants.SET_LOG_ENTRIES_COLLECTION,
            firestoreIds = setLogEntriesToDelete.mapNotNull { it.firestoreId },
        )

        workoutLogEntryDao.get(workoutLogEntryId)?.let { workoutLogEntryToDelete ->
            workoutLogEntryDao.delete(workoutLogEntryToDelete)

            if (workoutLogEntryToDelete.firestoreId != null) {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirebaseConstants.WORKOUT_LOG_ENTRIES_COLLECTION,
                    firestoreId = workoutLogEntryToDelete.firestoreId!!,
                )
            }
        }
    }

    suspend fun deleteSetLogEntryById(id: Long) {
        setLogEntryDao.get(id)?.let { toDelete ->
            setLogEntryDao.delete(toDelete)

            if (toDelete.firestoreId != null) {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirebaseConstants.SET_LOG_ENTRIES_COLLECTION,
                    firestoreId = toDelete.firestoreId!!,
                )
            }
        }
    }

    suspend fun upsert(workoutLogEntryId: Long, setLogEntry: SetLogEntryDto): Long {
        val current = setLogEntryDao.get(setLogEntry.id)
        val toUpsert = workoutLogEntryMapper.map(workoutLogEntryId, setLogEntry)
            .copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false
            )

        val id = setLogEntryDao.upsert(toUpsert).let {
            if (it == -1L) toUpsert.id else it
        }
        firestoreSyncManager.syncSingle(
            collectionName = FirebaseConstants.SET_LOG_ENTRIES_COLLECTION,
            entity = toUpsert.toFirebaseDto().copy(id = id),
            onSynced = { firestoreEntity ->
                setLogEntryDao.update(firestoreEntity.toEntity())
            }
        )

        return id
    }

    suspend fun upsertMany(workoutLogEntryId: Long, setLogEntries: List<SetLogEntryDto>): List<Long> {
        val currentEntries = setLogEntryDao.getMany(setLogEntries.map { it.id }).associateBy { it.id }
        var toUpsert = setLogEntries.fastMap { setLogEntry ->
            val current = currentEntries[setLogEntry.id]
            workoutLogEntryMapper.map(workoutLogEntryId, setLogEntry)
                .copyWithFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false
                )
        }
        val ids = setLogEntryDao.upsertMany(toUpsert)
        toUpsert = toUpsert.zip(ids).map {
            if (it.second == -1L) it.first else it.first.copy(id = it.second)
        }
        firestoreSyncManager.syncMany(
            collectionName = FirebaseConstants.SET_LOG_ENTRIES_COLLECTION,
            entities = toUpsert.map { it.toFirebaseDto() },
            onSynced = { firestoreEntities ->
                setLogEntryDao.updateMany(firestoreEntities.map { it.toEntity() })
            }
        )

        return ids
    }
}