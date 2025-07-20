package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.persistence.sync.BatchSyncQueueEntry
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.sync.SyncQueueEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

class WorkoutsRepository(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val programsRepository: ProgramsRepository,
    private val workoutMapper: WorkoutMapper,
    private val workoutsDao: WorkoutsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): Repository {
    suspend fun updateName(id: Long, newName: String) {
        val current = workoutsDao.get(id) ?: return
        val toUpdate = current.copy(name = newName).applyFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        workoutsDao.update(toUpdate)
        firestoreSyncManager.enqueueSyncRequest(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                roomEntityIds = listOf(toUpdate.id),
                SyncType.Upsert,
            )
        )
    }

    suspend fun insert(workout: WorkoutDto): Long {
        val toInsert = workoutMapper.map(workout)
        val id = workoutsDao.insert(toInsert)

        firestoreSyncManager.enqueueSyncRequest(
            entry = SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                roomEntityIds = listOf(id),
                syncType = SyncType.Upsert
            )
        )

        return id
    }

    suspend fun delete(workout: WorkoutDto) {
        val toDelete = workoutsDao.get(workout.id) ?: return
        workoutsDao.delete(toDelete)

        val syncQueueEntries = mutableListOf<SyncQueueEntry>()
        if (toDelete.firestoreId != null) {
            syncQueueEntries.add(
                SyncQueueEntry(
                    collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                    roomEntityIds = listOf(toDelete.id),
                    SyncType.Delete,
                )
            )
        }

        // Update workout positions
        val workoutsWithNewPositions = workoutsDao.getAllForProgram(workout.programId)
            .sortedBy { it.position }
            .fastMapIndexed { index, workoutEntity ->
                workoutEntity.copy(position = index)
            }
        workoutsDao.updateMany(workoutsWithNewPositions)

        syncQueueEntries.add(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                roomEntityIds = workoutsWithNewPositions.fastMap { it.id },
                SyncType.Upsert,
            )
        )

        // If current microcycle position is now greater than the number of workouts
        // set it to the last workout index
        programsRepository.getActive()?.let { program ->
            if (program.currentMicrocyclePosition > workoutsWithNewPositions.lastIndex) {
                val syncQueueEntry = programsRepository.updateMesoAndMicroCycleAndGetSyncQueueEntry(
                    id = program.id,
                    mesoCycle = program.currentMesocycle,
                    microCycle = program.currentMicrocycle,
                    microCyclePosition = workoutsWithNewPositions.lastIndex
                )

                if (syncQueueEntry != null) {
                    syncQueueEntries.add(syncQueueEntry)
                }
            }
        }

        if (syncQueueEntries.size == 1) {
            firestoreSyncManager.enqueueSyncRequest(
                syncQueueEntries.first()
            )
        } else if (syncQueueEntries.size > 1) {
            firestoreSyncManager.enqueueBatchSyncRequest(
                BatchSyncQueueEntry(
                    id = UUID.randomUUID().toString(),
                    batch = syncQueueEntries,
                )
            )
        }
    }

    suspend fun updateMany(workouts: List<WorkoutDto>) {
        val currentEntities = workoutsDao.getMany(workouts.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate = workouts.fastMapNotNull { workout ->
            val current = currentEntities[workout.id] ?: return@fastMapNotNull null
            workoutMapper.map(workout).copyWithFirestoreMetadata(
                firestoreId = current.firestoreId,
                lastUpdated = current.lastUpdated,
                synced = false,
            )
        }
        if (toUpdate.isEmpty()) return
        workoutsDao.updateMany(toUpdate)

        firestoreSyncManager.enqueueSyncRequest(
            entry = SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                roomEntityIds = toUpdate.fastMap { it.id },
                syncType = SyncType.Upsert
            )
        )
    }

    suspend fun update(workout: WorkoutDto) {
        val current = workoutsDao.get(workout.id) ?: return
        val updWorkout = workoutMapper.map(workout).copyWithFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        val updSets = workout.lifts
            .filterIsInstance<CustomWorkoutLiftDto>()
            .flatMap { lift ->
                lift.customLiftSets
            }

        workoutsDao.update(updWorkout)
        val workoutLiftSyncQueueEntry = workoutLiftsRepository.updateManyAndGetSyncQueueEntry(workout.lifts)
        val customLiftSetsSyncQueueEntry = customLiftSetsRepository.updateManyAndGetSyncQueueEntry(updSets)

        val syncQueueEntries = listOfNotNull(
            SyncQueueEntry(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                roomEntityIds = listOf(updWorkout.id),
                syncType = SyncType.Upsert
            ),
            workoutLiftSyncQueueEntry,
            customLiftSetsSyncQueueEntry
        )
        if (syncQueueEntries.size == 1) {
            firestoreSyncManager.enqueueSyncRequest(
                syncQueueEntries.first()
            )
        } else if (syncQueueEntries.size > 1) {
            firestoreSyncManager.enqueueBatchSyncRequest(
                BatchSyncQueueEntry(
                    id = UUID.randomUUID().toString(),
                    batch = syncQueueEntries,
                )
            )
        }
    }

    suspend fun get(workoutId: Long): WorkoutDto? {
        return workoutsDao.getWithRelationships(workoutId)?.let { workoutMapper.map(it) }
    }

    fun getFlow(workoutId: Long): Flow<WorkoutDto?> {
        return workoutsDao.getWithRelationshipsFlow(workoutId).map { workout ->
            workout?.let {
                workoutMapper.map(it)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getByMicrocyclePosition(
        programId: Long,
        microcyclePosition: Int,
    ): Flow<WorkoutDto?> {
        return workoutsDao.getByMicrocyclePosition(
            programId = programId,
            microcyclePosition = microcyclePosition,
        ).flatMapLatest { workoutEntity ->
            flowOf(
                workoutEntity?.let { workoutMapper.map(it) }
            )
        }
    }
}