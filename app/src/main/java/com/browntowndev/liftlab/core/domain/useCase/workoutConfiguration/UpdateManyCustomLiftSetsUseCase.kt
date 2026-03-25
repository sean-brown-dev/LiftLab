package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class UpdateManyCustomLiftSetsUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        workoutId: Long,
        sets: List<GenericLiftSet>
    ) = transactionScope.execute {
        val setsByWorkoutLift = sets.groupBy { it.workoutLiftId }
        val delta = programDelta {
            workout(workoutId) {
                setsByWorkoutLift.forEach { (workoutLiftId, sets) ->
                    updateSets(workoutLiftId) {
                        sets.fastForEach { set ->
                            set(set)
                        }
                    }
                }
            }
        }

        programsRepository.applyDelta(programId, delta)
    }
}