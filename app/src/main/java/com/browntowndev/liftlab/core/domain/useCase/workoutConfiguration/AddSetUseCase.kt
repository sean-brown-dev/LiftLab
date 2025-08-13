package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class AddSetUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        workoutLift: CustomWorkoutLift,
    ) = transactionScope.execute {
        val delta = programDelta {
            workout(workoutLift.workoutId) {
                lift(workoutLift.id) {
                    set(
                        StandardSet(
                            workoutLiftId = workoutLift.id,
                            position = workoutLift.customLiftSets.size,
                            rpeTarget = 8f,
                            repRangeBottom = 8,
                            repRangeTop = 10
                        )
                    )
                }
            }
        }

        programsRepository.applyDelta(programId, delta)
    }
}