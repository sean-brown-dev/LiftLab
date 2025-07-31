package com.browntowndev.liftlab.core.domain.repositories

import com.browntowndev.liftlab.core.domain.models.metadata.ActiveProgramMetadata
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import kotlinx.coroutines.flow.Flow

interface ProgramsRepository: Repository<Program, Long> {
    suspend fun getActive(): Program?
    fun getActiveProgramFlow(): Flow<Program?>
    suspend fun updateName(id: Long, newName: String)
    suspend fun updateDeloadWeek(id: Long, newDeloadWeek: Int)
    suspend fun getDeloadWeek(id: Long): Int
    fun getActiveProgramMetadataFlow(): Flow<ActiveProgramMetadata?>
    suspend fun updateMesoAndMicroCycle(id: Long, mesoCycle: Int, microCycle: Int, microCyclePosition: Int)
}