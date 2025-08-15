package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDeltaSuspend
import com.browntowndev.liftlab.core.domain.models.interfaces.SetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LoggingWorkout
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class ReorderWorkoutLiftsUseCase(
    private val programsRepository: ProgramsRepository,
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val setResultsRepository: LiveWorkoutCompletedSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        workout: LoggingWorkout,
        completedSets: List<SetResult>,
        newWorkoutLiftIndices: Map<Long, Int>,
    ) = transactionScope.execute {
        val delta = programDeltaSuspend {
            workoutSuspend(workout.id) {
                workoutLiftsRepository.getForWorkout(workout.id)
                    .fastForEach {
                        updateLift(workoutLiftId = it.id, position = Patch.Set(newWorkoutLiftIndices[it.id]!!))
                    }
            }
        }
        programsRepository.applyDelta(programId, delta)

        if (completedSets.isEmpty()) return@execute
        val workoutLiftIdByLiftId = workout.lifts.associate { it.liftId to it.id }
        val updatedInProgressSetResults = completedSets.map { completedSet ->
                val workoutLiftIdOfCompletedSet = workoutLiftIdByLiftId[completedSet.liftId]
                when (completedSet) {
                    is StandardSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                    is MyoRepSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                    is LinearProgressionSetResult -> completedSet.copy(liftPosition = newWorkoutLiftIndices[workoutLiftIdOfCompletedSet]!!)
                    else -> throw Exception("${completedSet::class.simpleName} is not defined.")
                }
            }
        setResultsRepository.upsertMany(updatedInProgressSetResults)
    }
}