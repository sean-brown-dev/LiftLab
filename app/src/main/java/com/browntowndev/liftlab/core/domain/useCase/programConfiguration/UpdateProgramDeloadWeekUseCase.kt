package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.extensions.getAllLiftsWithRecalculatedStepSize

class UpdateProgramDeloadWeekUseCase(
    private val programsRepository: ProgramsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(program: Program, deloadWeek: Int, useLiftSpecificDeload: Boolean) = transactionScope.execute {
        programsRepository.updateDeloadWeek(program.id, deloadWeek)
        val liftsWithNewStepSizes: Map<Long, StandardWorkoutLift> = program.workouts.getAllLiftsWithRecalculatedStepSize(
            deloadToUseInsteadOfLiftLevel = if(useLiftSpecificDeload) null else deloadWeek,
        )

        if (liftsWithNewStepSizes.isNotEmpty()) {
            workoutLiftsRepository.updateMany(liftsWithNewStepSizes.values.toList())
        }
    }
}