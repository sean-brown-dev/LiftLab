package com.browntowndev.liftlab.core.persistence.room.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import androidx.compose.ui.util.fastMapNotNull
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.enums.SyncType
import com.browntowndev.liftlab.core.domain.models.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.Workout
import com.browntowndev.liftlab.core.domain.mapping.WorkoutMappingExtensions.toDomainModel
import com.browntowndev.liftlab.core.domain.mapping.WorkoutMappingExtensions.toEntity
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository
import com.browntowndev.liftlab.core.persistence.room.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.entities.applyFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.firestore.sync.BatchSyncQueueEntry
import com.browntowndev.liftlab.core.persistence.firestore.sync.FirestoreSyncManager
import com.browntowndev.liftlab.core.persistence.firestore.sync.SyncQueueEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class WorkoutsRepositoryImpl(
    private val workoutLiftsRepositoryImpl: WorkoutLiftsRepositoryImpl,
    private val customLiftSetsRepositoryImpl: CustomLiftSetsRepositoryImpl,
    private val programsRepository: ProgramsRepository,
    private val workoutsDao: WorkoutsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
): WorkoutsRepository {
    override suspend fun updateName(id: Long, newName: String) {
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

    override suspend fun insert(model: Workout): Long {
        val toInsert = model.toEntity()
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

    override suspend fun insertMany(models: List<Workout>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(model: Workout): Int {
        val toDelete = workoutsDao.get(model.id) ?: return 0
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

        // Update workoutEntity positions
        val workoutsWithNewPositions = workoutsDao.getAllForProgram(model.programId)
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
        // set it to the last workoutEntity index
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
        return 1
    }

    override suspend fun deleteMany(models: List<Workout>): Int {
        TODO("Not yet implemented")
    }

    override suspend fun deleteById(id: Long): Int {
        TODO("Not yet implemented")
    }

    override suspend fun updateMany(models: List<Workout>) {
        val currentEntities = workoutsDao.getMany(models.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate = models.fastMapNotNull { workout ->
            val current = currentEntities[workout.id] ?: return@fastMapNotNull null
            workout.toEntity().copyWithFirestoreMetadata(
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

    override suspend fun upsert(model: Workout): Long {
        TODO("Not yet implemented")
    }

    override suspend fun upsertMany(models: List<Workout>): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun update(model: Workout) {
        val current = workoutsDao.get(model.id) ?: return
        val updWorkout = model.toEntity().copyWithFirestoreMetadata(
            firestoreId = current.firestoreId,
            lastUpdated = current.lastUpdated,
            synced = false,
        )
        val updSets = model.lifts
            .filterIsInstance<CustomWorkoutLift>()
            .flatMap { lift ->
                lift.customLiftSets
            }

        workoutsDao.update(updWorkout)
        val workoutLiftSyncQueueEntry = workoutLiftsRepositoryImpl.updateManyAndGetSyncQueueEntry(model.lifts)
        val customLiftSetsSyncQueueEntry = customLiftSetsRepositoryImpl.updateManyAndGetSyncQueueEntry(updSets)

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

    override suspend fun getAll(): List<Workout> {
        TODO("Not yet implemented")
    }

    override suspend fun getById(id: Long): Workout? {
        return workoutsDao.getWithRelationships(id)?.toDomainModel()
    }

    override suspend fun getMany(ids: List<Long>): List<Workout> {
        TODO("Not yet implemented")
    }

    override fun getFlow(workoutId: Long): Flow<Workout?> {
        return workoutsDao.getWithRelationshipsFlow(workoutId).map { workout ->
            workout?.toDomainModel()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getByMicrocyclePosition(
        programId: Long,
        microcyclePosition: Int,
    ): Flow<Workout?> {
        return workoutsDao.getByMicrocyclePosition(
            programId = programId,
            microcyclePosition = microcyclePosition,
        ).map { workoutEntity ->
            workoutEntity?.toDomainModel()
        }
    }
}