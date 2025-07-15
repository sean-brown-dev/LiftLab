package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import com.browntowndev.liftlab.core.common.FirestoreConstants
import com.browntowndev.liftlab.core.common.fireAndForgetSync
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toEntity
import com.browntowndev.liftlab.core.persistence.mapping.FirebaseMappers.toFirestoreDto
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.persistence.sync.FirestoreSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class WorkoutsRepository(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val programsRepository: ProgramsRepository,
    private val workoutMapper: WorkoutMapper,
    private val workoutsDao: WorkoutsDao,
    private val firestoreSyncManager: FirestoreSyncManager,
    private val syncScope: CoroutineScope,
): Repository {
    suspend fun updateName(id: Long, newName: String) {
        val toUpdate = workoutsDao.get(id)?.copy(name = newName) ?: return
        workoutsDao.update(toUpdate)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                entity = toUpdate.toFirestoreDto(),
                onSynced = {
                    workoutsDao.update(it.toEntity())
                }
            )
        }
    }

    suspend fun insert(workout: WorkoutDto): Long {
        val toInsert = workoutMapper.map(workout)
        val id = workoutsDao.insert(toInsert)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                entity = toInsert.toFirestoreDto().copy(id = id),
                onSynced = {
                    workoutsDao.update(it.toEntity())
                }
            )
        }

        return id
    }

    suspend fun delete(workout: WorkoutDto) {
        val toDelete = workoutsDao.get(workout.id) ?: return
        workoutsDao.delete(toDelete)
        if (toDelete.firestoreId != null) {
            syncScope.fireAndForgetSync {
                firestoreSyncManager.deleteSingle(
                    collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                    firestoreId = toDelete.firestoreId!!,
                )
            }
        }

        // Update workout positions
        val workoutsWithNewPositions = workoutsDao.getAllForProgram(workout.programId)
            .sortedBy { it.position }
            .fastMapIndexed { index, workoutEntity ->
                workoutEntity.copy(position = index)
            }
        workoutsDao.updateMany(workoutsWithNewPositions)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncMany(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                entities = workoutsWithNewPositions.map { it.toFirestoreDto() },
                onSynced = { firestoreEntities ->
                    workoutsDao.updateMany(firestoreEntities.map { entity -> entity.toEntity() })
                }
            )
        }

        // If current microcycle position is now greater than the number of workouts
        // set it to the last workout index
        programsRepository.getActiveNotAsLiveData()?.let { program ->
            if (program.currentMicrocyclePosition > workoutsWithNewPositions.lastIndex) {
                programsRepository.updateMesoAndMicroCycle(
                    id = program.id,
                    mesoCycle = program.currentMesocycle,
                    microCycle = program.currentMicrocycle,
                    microCyclePosition = workoutsWithNewPositions.lastIndex
                )
            }
        }
    }

    suspend fun updateMany(workouts: List<WorkoutDto>) {
        val currentEntities = workoutsDao.getMany(workouts.map { it.id }).associateBy { it.id }
        if (currentEntities.isEmpty()) return

        val toUpdate = workouts.fastMap { workout ->
            val current = currentEntities[workout.id]
            workoutMapper.map(workout).copyWithFirestoreMetadata(
                firestoreId = current?.firestoreId,
                lastUpdated = current?.lastUpdated,
                synced = false,
            )
        }
        workoutsDao.updateMany(toUpdate)

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncMany(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                entities = toUpdate.map { it.toFirestoreDto() },
                onSynced = { firestoreEntities ->
                    workoutsDao.updateMany(firestoreEntities.map { entity -> entity.toEntity() })
                }
            )
        }
    }

    suspend fun update(workout: WorkoutDto) {
        val current = workoutsDao.get(workout.id)
        if (current == null) return

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

        syncScope.fireAndForgetSync {
            firestoreSyncManager.syncSingle(
                collectionName = FirestoreConstants.WORKOUTS_COLLECTION,
                entity = updWorkout.toFirestoreDto(),
                onSynced = {
                    workoutsDao.update(it.toEntity())
                }
            )
        }

        workoutLiftsRepository.updateMany(workout.lifts)
        customLiftSetsRepository.updateMany(updSets)
    }

    suspend fun get(workoutId: Long): WorkoutDto? {
        return workoutsDao.getWithRelationships(workoutId)?.let {
             workoutMapper.map(it)
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