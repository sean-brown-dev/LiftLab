package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.extensions.getRecalculatedStepSizeForLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class UpdateWorkoutLiftDeloadWeekUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        workoutLift: GenericWorkoutLift,
        deloadWeek: Int?,
        programDeloadWeek: Int
    ) = transactionScope.execute {
        val delta = programDelta {
            workout(workoutLift.workoutId) {
                lift(
                    workoutLiftId = workoutLift.id,
                    deloadWeek = deloadWeek,
                    stepSize = (workoutLift as? StandardWorkoutLift)?.getRecalculatedStepSizeForLift(
                        deloadToUseInsteadOfLiftLevel = deloadWeek ?: programDeloadWeek,
                    )
                )
            }
        }

        programsRepository.applyDelta(programId, delta)
    }
}