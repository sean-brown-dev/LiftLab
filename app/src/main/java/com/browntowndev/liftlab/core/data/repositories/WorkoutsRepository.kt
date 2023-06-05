package com.browntowndev.liftlab.core.data.repositories

import com.browntowndev.liftlab.core.data.dao.WorkoutsDao
import com.browntowndev.liftlab.core.data.dtos.ProgramDto
import com.browntowndev.liftlab.core.data.entities.Workout

class WorkoutsRepository(private val workoutsDao: WorkoutsDao): Repository {
    suspend fun updateName(id: Long, newName: String) {
        workoutsDao.updateName(id, newName)
    }

    suspend fun create(programId: Long, name: String) {
        val position = workoutsDao.getFinalPosition(programId = programId) + 1
        workoutsDao.insert(Workout(programId = programId, name = name, position = position))
    }

    suspend fun delete(workoutId: Long) {
        workoutsDao.delete(workoutId)
    }

    suspend fun delete(workoutDto: ProgramDto.WorkoutDto) {
        workoutsDao.delete(workoutDto.workout)
    }

    suspend fun updateMany(workouts: List<ProgramDto.WorkoutDto>) {
        workoutsDao.updateMany(workouts.map {
            it.workout.copy(
                id = it.id,
                programId = it.programId,
                name = it.name,
                position = it.position
            )
        })
    }
}