package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics

class SetProgramAsActiveUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(idOfProgramToActivate: Long, allPrograms: List<Program>) = transactionScope.execute {
        val programToActivate = allPrograms.find { it.id == idOfProgramToActivate } ?: throw IllegalArgumentException("Program not found")
        programsRepository.update(programToActivate.copy(isActive = true))

        val programsToDeactivate = allPrograms
            .filter { it.isActive && it.id != idOfProgramToActivate }

        if (programsToDeactivate.size > 1) {
            FirebaseCrashlytics.getInstance().recordException(Exception("Multiple programs were active at once. $programsToDeactivate"))
        }

        if (programsToDeactivate.isNotEmpty()) {
            programsRepository.updateMany(
                models = programsToDeactivate.fastMap { it.copy(isActive = false) }
            )
        }
    }
}