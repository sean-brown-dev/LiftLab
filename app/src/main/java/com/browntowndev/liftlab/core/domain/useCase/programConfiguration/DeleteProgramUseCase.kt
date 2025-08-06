package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class DeleteProgramUseCase(
    private val programsRepository: ProgramsRepository,
    private val workoutsRepository: WorkoutsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val customLiftSetsRepository: CustomLiftSetsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(programId: Long) = transactionScope.execute {
        val programToDelete = programsRepository.getById(programId) ?: return@execute
        if (programToDelete.isActive) {
            workoutInProgressRepository.deleteAll()
        }
        programsRepository.delete(programToDelete)
        workoutsRepository.deleteByProgramId(programId)
        workoutLiftsRepository.deleteByProgramId(programId)
        customLiftSetsRepository.deleteByProgramId(programId)


        val allPrograms = programsRepository.getAll()
        if (!allPrograms.isEmpty() && !allPrograms.any { it.isActive }) {
            val newActiveProgram = allPrograms.first().copy(isActive = true)
            programsRepository.update(newActiveProgram)
        }
    }
}