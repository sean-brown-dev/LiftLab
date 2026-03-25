package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics

class SetProgramAsActiveUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(idOfProgramToActivate: Long, allPrograms: List<Program>) = transactionScope.execute {
        val programToActivate = allPrograms.find { it.id == idOfProgramToActivate } ?: throw IllegalArgumentException("Program not found")
        val activateProgramDelta = programDelta {
            updateProgram(isActive = Patch.Set(true))
        }
        programsRepository.applyDelta(programToActivate.id, activateProgramDelta)

        val programsToDeactivate = allPrograms
            .filter { it.isActive && it.id != idOfProgramToActivate }

        if (programsToDeactivate.size > 1) {
            FirebaseCrashlytics.getInstance().recordException(Exception("Multiple programs were active at once. $programsToDeactivate"))
        }

        programsToDeactivate.fastForEach { program ->
            val deactivateDelta = programDelta {
                updateProgram(isActive = Patch.Set(false))
            }
            programsRepository.applyDelta(program.id, deactivateDelta)
        }
    }
}