package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.CustomSetsDao
import com.browntowndev.liftlab.core.persistence.dao.WorkoutsDao
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.mapping.CustomLiftSetMapper
import com.browntowndev.liftlab.core.persistence.mapping.WorkoutMapper

class WorkoutsRepository(
    private val workoutsDao: WorkoutsDao,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customSetsDao: CustomSetsDao,
    private val workoutMapper: WorkoutMapper,
    private val customLiftSetMapper: CustomLiftSetMapper,
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
                lift.customLiftSets.map { set ->
                    customLiftSetMapper.map(set)
                }
            }

        workoutsDao.update(updWorkout)
        workoutLiftsRepository.updateMany(workout.lifts)
        customSetsDao.updateMany(updSets)
    }
}