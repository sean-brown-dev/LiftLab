package com.browntowndev.liftlab.core.data.repositories

import com.browntowndev.liftlab.core.data.dao.ProgramsDao
import com.browntowndev.liftlab.core.data.dtos.ProgramDto

class ProgramsRepository(private val programsDao: ProgramsDao) : Repository {
    suspend fun getAll(): List<ProgramDto> {
        val programs = programsDao.getAll().sortedByDescending { it.id }
        programs.forEach { program ->
            program.workouts = program.workouts.sortedBy { it.position }
            program.workouts.forEach { workout ->
                workout.lifts = workout.lifts.sortedBy { it.position }
                workout.lifts.sortedBy { it.position }
            }
        }

        return programs
    }
}