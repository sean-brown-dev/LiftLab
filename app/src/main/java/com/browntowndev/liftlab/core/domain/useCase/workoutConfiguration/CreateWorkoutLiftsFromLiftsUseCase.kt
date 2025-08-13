package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.workout.Lift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class CreateWorkoutLiftsFromLiftsUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(workoutId: Long, firstPosition: Int, lifts: List<Lift>) = transactionScope.execute {
        if (lifts.isEmpty()) return@execute

        var position = firstPosition
        val newLifts = lifts.fastMap { lift ->
            val newWorkoutLift = StandardWorkoutLift(
                liftId = lift.id,
                workoutId = workoutId,
                liftName = lift.name,
                liftMovementPattern = lift.movementPattern,
                liftVolumeTypes = lift.volumeTypesBitmask,
                liftSecondaryVolumeTypes = lift.secondaryVolumeTypesBitmask,
                position = position,
                deloadWeek = null,
                liftNote = null,
                setCount = 3,
                incrementOverride = lift.incrementOverride,
                restTime = lift.restTime,
                restTimerEnabled = lift.restTimerEnabled,
                rpeTarget = 8f,
                repRangeBottom = 8,
                repRangeTop = 10,
                progressionScheme = ProgressionScheme.DOUBLE_PROGRESSION,
            )

            position++
            newWorkoutLift
        }

        val delta = programDelta {
            workout(workoutId) {
                newLifts.fastForEach { lift ->
                    insertLift(lift)
                }
            }
        }
        val programId = programsRepository.getForWorkout(workoutId)?.id ?: error("No program found for workout")
        programsRepository.applyDelta(programId, delta)
    }
}