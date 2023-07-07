package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutWithProgressionDto
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper
import com.browntowndev.liftlab.core.progression.ProgressionFactory

class WorkoutsRepository(
    private val programsRepository: ProgramsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val previousSetResultsRepository: PreviousSetResultsRepository,
    private val workoutMapper: WorkoutMapper,
    private val workoutsDao: WorkoutsDao,
    private val progressionFactory: ProgressionFactory,
): Repository {
    suspend fun updateName(id: Long, newName: String) {
        workoutsDao.updateName(id, newName)
    }

    suspend fun get(id: Long): WorkoutDto {
        return workoutMapper.map(workoutsDao.get(id))
    }

    suspend fun insert(workout: WorkoutDto): Long {
        return workoutsDao.insert(workout = workoutMapper.map(workout))
    }

    suspend fun delete(workout: WorkoutDto) {
        workoutsDao.delete(workoutMapper.map(workout))
    }

    suspend fun updateMany(workouts: List<WorkoutDto>) {
        workoutsDao.updateMany(workouts.map { workoutMapper.map(it) })
    }

    suspend fun update(workout: WorkoutDto) {
        val updWorkout = workoutMapper.map(workout)
        val updSets = workout.lifts
            .filterIsInstance<CustomWorkoutLiftDto>()
            .flatMap { lift ->
                lift.customLiftSets
            }

        workoutsDao.update(updWorkout)
        workoutLiftsRepository.updateMany(workout.lifts)
        customLiftSetsRepository.updateMany(updSets)
    }

    suspend fun getNextToPerform(programMetadata: ActiveProgramMetadataDto): WorkoutWithProgressionDto {
        val workout = workoutsDao.getByMicrocyclePosition(programMetadata.currentMicrocyclePosition)
        val previousSetResults = previousSetResultsRepository.getByWorkoutId(workout.workout.id)

        return progressionFactory.calculate(
            programMetadata.deloadWeek,
            workoutMapper.map(workout),
            previousSetResults
        )
    }
}