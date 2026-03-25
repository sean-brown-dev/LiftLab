package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.enums.ProgressionScheme
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.utils.getPossibleStepSizes

class UpdateWorkoutLiftUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        programDeloadWeek: Int,
        workoutLift: GenericWorkoutLift,
    ) = transactionScope.execute {

        val workoutLiftToUpdate =
            if (workoutLift is StandardWorkoutLift &&
                workoutLift.progressionScheme == ProgressionScheme.WAVE_LOADING_PROGRESSION &&
                workoutLift.stepSize == null
            ) {
                val stepSizeOptions = getPossibleStepSizes(
                    repRangeTop = workoutLift.repRangeTop,
                    repRangeBottom = workoutLift.repRangeBottom,
                    stepCount = workoutLift.deloadWeek?.let { it - 2 } ?: programDeloadWeek
                )
                workoutLift.copy(stepSize = stepSizeOptions.firstOrNull())
            } else workoutLift

        val delta = programDelta {
            workout(workoutLiftToUpdate.workoutId) {
                val stdWorkoutLift = workoutLiftToUpdate as? StandardWorkoutLift
                val customWorkoutLift = workoutLiftToUpdate as? CustomWorkoutLift
                updateLift(
                    workoutLiftId = workoutLiftToUpdate.id,
                    liftId = Patch.Set(workoutLiftToUpdate.liftId),
                    position = Patch.Set(workoutLiftToUpdate.position),
                    setCount = Patch.Set(workoutLiftToUpdate.setCount),
                    progressionScheme = Patch.Set(workoutLiftToUpdate.progressionScheme),
                    deloadWeek = Patch.Set(workoutLiftToUpdate.deloadWeek),
                    incrementOverride = Patch.Set(workoutLiftToUpdate.incrementOverride),
                    restTime = Patch.Set(workoutLiftToUpdate.restTime),
                    restTimerEnabled = Patch.Set(workoutLiftToUpdate.restTimerEnabled),
                    repRangeTop = Patch.Set(stdWorkoutLift?.repRangeTop),
                    repRangeBottom = Patch.Set(stdWorkoutLift?.repRangeBottom),
                    rpeTarget = Patch.Set(stdWorkoutLift?.rpeTarget),
                    stepSize = Patch.Set(stdWorkoutLift?.stepSize),
                    volumeCyclingSetCeiling = Patch.Set(stdWorkoutLift?.volumeCyclingSetCeiling),
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