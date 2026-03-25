package com.browntowndev.liftlab.core.domain.useCase.liftConfiguration

import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.repositories.LiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.SetLogEntryRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class MergeLiftsUseCase(
    private val liftRepository: LiftsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository,
    private val setLogEntryRepository: SetLogEntryRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(liftToMergeInto: Long, liftsToMerge: List<Long>) = transactionScope.execute {
        liveWorkoutCompletedSetsRepository.changeFromLiftsToNewLift(liftToMergeInto, liftsToMerge)
        setLogEntryRepository.changeFromLiftsToNewLift(liftToMergeInto, liftsToMerge)
        workoutLiftsRepository.changeFromLiftsToNewLift(liftToMergeInto, liftsToMerge)
        liftRepository.deleteManyById(liftsToMerge)
    }
}