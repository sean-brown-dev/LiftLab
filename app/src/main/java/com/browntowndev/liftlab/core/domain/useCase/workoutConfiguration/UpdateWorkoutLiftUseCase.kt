package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class UpdateWorkoutLiftUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        workoutLift: GenericWorkoutLift
    ) = transactionScope.execute {
        val delta = programDelta {
            workout(workoutLift.workoutId) {
                val stdWorkoutLift = workoutLift as? StandardWorkoutLift
                val customWorkoutLift = workoutLift as? CustomWorkoutLift
                updateLift(
                    workoutLiftId = workoutLift.id,
                    liftId = workoutLift.liftId,
                    position = workoutLift.position,
                    setCount = workoutLift.setCount,
                    progressionScheme = workoutLift.progressionScheme,
                    deloadWeek = workoutLift.deloadWeek,
                    incrementOverride = workoutLift.incrementOverride,
                    restTime = workoutLift.restTime,
                    restTimerEnabled = workoutLift.restTimerEnabled,
                    repRangeTop = stdWorkoutLift?.repRangeTop,
                    repRangeBottom = stdWorkoutLift?.repRangeBottom,
                    rpeTarget = stdWorkoutLift?.rpeTarget,
                    stepSize = stdWorkoutLift?.stepSize,
                ) {
                    customWorkoutLift?.customLiftSets?.forEach { set ->
                        set(set)
                    }
                }
            }
        }

        programsRepository.applyDelta(programId, delta)
    }
}