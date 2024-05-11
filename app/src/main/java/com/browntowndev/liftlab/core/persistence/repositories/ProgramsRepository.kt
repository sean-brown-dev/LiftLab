package com.browntowndev.liftlab.core.persistence.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.browntowndev.liftlab.core.persistence.dao.ProgramsDao
import com.browntowndev.liftlab.core.persistence.dtos.ActiveProgramMetadataDto
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.mapping.ProgramMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ProgramsRepository(
    private val programsDao: ProgramsDao,
    private val programMapper: ProgramMapper
) : Repository {
    suspend fun getAll(): List<ProgramDto> {
        return programsDao.getAll().map { programMapper.map(it) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getActive(): LiveData<ProgramDto?> {
        val programMeta = programsDao.getActive().flatMapLatest { programEntity ->
            flowOf(
                if (programEntity != null) {
                    val program = programMapper.map(programEntity)
                    // Sort the workout and lift positions
                    program.copy(workouts = program.workouts
                        .sortedBy { workout -> workout.position }
                        .map { workout ->
                            workout.copy(lifts = workout.lifts
                                .sortedBy { lift -> lift.position }
                                .map { lift ->
                                    when (lift) {
                                        is CustomWorkoutLiftDto -> lift.copy(
                                            customLiftSets = lift.customLiftSets.sortedBy { it.position }
                                        )
                                        else -> lift
                                    }
                                }
                            )
                        }
                    )
                } else null
            )
        }.asLiveData()

        return programMeta
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

    suspend fun updateMany(programs: List<ProgramDto>) {
        programsDao.updateMany(
            programs.map { programMapper.map(it) }
        )
    }

    suspend fun insert(program: ProgramDto): Long {
        return programsDao.insert(programMapper.map(program))
    }

    suspend fun getDeloadWeek(id: Long): Int {
        return programsDao.getDeloadWeek(id)
    }

    fun getActiveProgramMetadata(): LiveData<ActiveProgramMetadataDto?> {
        return programsDao.getActiveProgramMetadata().asLiveData()
    }

    suspend fun delete(id: Long) {
        programsDao.delete(id)
    }

    suspend fun delete(programToDelete: ProgramDto) {
        programsDao.delete(programMapper.map(programToDelete))
    }

    suspend fun updateMesoAndMicroCycle(id: Long, mesoCycle: Int, microCycle: Int, microCyclePosition: Int) {
        programsDao.updateMesoAndMicroCycle(id, mesoCycle, microCycle, microCyclePosition)
    }
}