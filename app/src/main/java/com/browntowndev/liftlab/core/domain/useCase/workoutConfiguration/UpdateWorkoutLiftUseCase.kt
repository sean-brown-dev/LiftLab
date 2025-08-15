package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.common.Patch
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
                    liftId = Patch.Set(workoutLift.liftId),
                    position = Patch.Set(workoutLift.position),
                    setCount = Patch.Set(workoutLift.setCount),
                    progressionScheme = Patch.Set(workoutLift.progressionScheme),
                    deloadWeek = Patch.Set(workoutLift.deloadWeek),
                    incrementOverride = Patch.Set(workoutLift.incrementOverride),
                    restTime = Patch.Set(workoutLift.restTime),
                    restTimerEnabled = Patch.Set(workoutLift.restTimerEnabled),
                    repRangeTop = Patch.Set(stdWorkoutLift?.repRangeTop),
                    repRangeBottom = Patch.Set(stdWorkoutLift?.repRangeBottom),
                    rpeTarget = Patch.Set(stdWorkoutLift?.rpeTarget),
                    stepSize = Patch.Set(stdWorkoutLift?.stepSize),
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