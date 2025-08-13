package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.delta.ProgramDelta
import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import kotlinx.coroutines.flow.Flow

interface ProgramsRepository {
    fun getAllFlow(): Flow<List<Program>>
    suspend fun getNewest(): Program?
    suspend fun getForWorkout(workoutId: Long): Program?
    suspend fun insert(program: Program): Long
    suspend fun applyDelta(programId: Long, delta: ProgramDelta)
    suspend fun getActive(): Program?
    fun getActiveProgramFlow(): Flow<Program?>
    suspend fun getDeloadWeek(id: Long): Int
    fun getActiveProgramMetadataFlow(): Flow<ActiveProgramMetadata?>
}