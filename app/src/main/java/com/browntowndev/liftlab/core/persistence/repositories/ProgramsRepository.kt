package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.ProgramWithRelationships
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper

class ProgramsRepository(private val programsDao: ProgramsDao, private val programMapper: ProgramMapper) : Repository {
    suspend fun getActive(): ProgramDto? {
        val programEntity: ProgramWithRelationships? = programsDao.getActive()
        var program: ProgramDto? = null;

        if (programEntity != null) {
            program = programMapper.map(programEntity)

            // Sort the workout and lift positions
            program = program.copy(workouts = program.workouts
                .sortedBy { workout -> workout.position }
                .map { workout ->
                    workout.copy(lifts = workout.lifts.sortedBy { lift -> lift.position })
                }
            )
        }

        return program
    }

    suspend fun updateName(id: Long, newName: String) {
        programsDao.updateName(id, newName)
    }

    suspend fun delete(id: Long) {
        programsDao.delete(id)
    }

    suspend fun delete(programToDelete: ProgramDto) {
        programsDao.delete(programMapper.map(programToDelete))
    }
}