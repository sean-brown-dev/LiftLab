package com.browntowndev.liftlab.core.persistence.repositories

import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMapIndexed
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.entities.copyWithFirestoreMetadata
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
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
): Repository {
    suspend fun updateName(id: Long, newName: String) {
        workoutsDao.updateName(id, newName)
    }

    suspend fun insert(workout: WorkoutDto): Long {
        return workoutsDao.insert(workoutMapper.map(workout))
    }

    suspend fun delete(workout: WorkoutDto) {
        workoutsDao.delete(workoutMapper.map(workout))

        // Update workout positions
        val workoutsWithNewPositions = workoutsDao.getAllForProgram(workout.programId)
            .sortedBy { it.position }
            .fastMapIndexed { index, workoutEntity ->
                workoutEntity.copy(position = index)
            }
        workoutsDao.updateMany(workoutsWithNewPositions)

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
        workoutsDao.updateMany(
            workouts.fastMap { workout ->
                val current = currentEntities[workout.id]
                workoutMapper.map(workout).copyWithFirestoreMetadata(
                    firestoreId = current?.firestoreId,
                    lastUpdated = current?.lastUpdated,
                    synced = false,
                )
            }
        )
    }

    suspend fun update(workout: WorkoutDto) {
        val current = workoutsDao.get(workout.id)
        val updWorkout = workoutMapper.map(workout).copyWithFirestoreMetadata(
            firestoreId = current?.firestoreId,
            lastUpdated = current?.lastUpdated,
            synced = false,
        )
        val updSets = workout.lifts
            .filterIsInstance<CustomWorkoutLiftDto>()
            .flatMap { lift ->
                lift.customLiftSets
            }

        workoutsDao.update(updWorkout)
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