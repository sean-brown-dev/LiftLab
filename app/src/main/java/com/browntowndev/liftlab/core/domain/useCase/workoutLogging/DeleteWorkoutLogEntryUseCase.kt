package com.browntowndev.liftlab.core.domain.useCase.workoutLogging

import android.util.Log
import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.workoutLogging.WorkoutLogEntry
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.RestTimerInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLogRepository

class DeleteWorkoutLogEntryUseCase(
    private val workoutLogRepository: WorkoutLogRepository,
    private val programsRepository: ProgramsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val restTimerInProgressRepository: RestTimerInProgressRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutLogEntry: WorkoutLogEntry, isMostRecentWorkout: Boolean) = transactionScope.execute {
        workoutLogRepository.deleteById(workoutLogEntry.id)

        if (isMostRecentWorkout) {
            Log.d("DeleteWorkoutLogEntryUseCase", "Is most recent workout")
            val activeProgramMetadata = programsRepository.getActive()
            if (activeProgramMetadata?.id == workoutLogEntry.programId) {
                Log.d("DeleteWorkoutLogEntryUseCase", "Is active program")
                val delta = programDelta {
                    updateProgram(
                        currentMesocycle = Patch.Set(workoutLogEntry.mesocycle),
                        currentMicrocycle = Patch.Set(workoutLogEntry.microcycle),
                        currentMicrocyclePosition = Patch.Set(workoutLogEntry.microcyclePosition)
                    )
                }
                programsRepository.applyDelta(activeProgramMetadata.id, delta)
                Log.d("DeleteWorkoutLogEntryUseCase", "Updated program: $delta")

                workoutInProgressRepository.delete()
                restTimerInProgressRepository.delete()
            }
        }
    }
}