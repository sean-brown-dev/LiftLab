package com.browntowndev.liftlab.core.persistence.repositories

import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.queryable.ProgramWithRelationships
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper

class ProgramsRepository(private val programsDao: ProgramsDao, private val programMapper: ProgramMapper) : Repository {
    suspend fun getActive(): ProgramDto? {
        val programEntity: ProgramWithRelationships? = programsDao.getActive()
        var program: ProgramDto? = null

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

    suspend fun updateDeloadWeek(id: Long, newDeloadWeek: Int) {
        programsDao.updateDeloadWeek(id, newDeloadWeek)
    }

    suspend fun update(program: ProgramDto) {
        programsDao.update(programMapper.map(program))
    }

    suspend fun insert(program: ProgramDto): Long {
        return programsDao.insert(programMapper.map(program))
    }

    suspend fun getDeloadWeek(id: Long): Int {
        return programsDao.getDeloadWeek(id)
    }

    suspend fun delete(id: Long) {
        programsDao.delete(id)
    }

    suspend fun delete(programToDelete: ProgramDto) {
        programsDao.delete(programMapper.map(programToDelete))
    }
}