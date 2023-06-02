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
                workout.lifts.forEach { lift ->
                    lift.customLiftSets = lift.customLiftSets.sortedBy { it.position }
                }
            }
        }

        return programs
    }

    suspend fun updateName(id: Long, newName: String) {
        programsDao.updateName(id, newName)
    }

    suspend fun delete(id: Long) {
        programsDao.delete(id)
    }

    suspend fun delete(programDto: ProgramDto) {
        programsDao.delete(programDto.program)
    }
}