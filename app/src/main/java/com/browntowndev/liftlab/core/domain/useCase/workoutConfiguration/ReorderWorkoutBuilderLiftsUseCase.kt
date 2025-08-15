package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workoutLogging.LinearProgressionSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.MyoRepSetResult
import com.browntowndev.liftlab.core.domain.models.workoutLogging.StandardSetResult
import com.browntowndev.liftlab.core.domain.repositories.LiveWorkoutCompletedSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutInProgressRepository

class ReorderWorkoutBuilderLiftsUseCase(
    private val programsRepository: ProgramsRepository,
    private val workoutInProgressRepository: WorkoutInProgressRepository,
    private val liveWorkoutCompletedSetsRepository: LiveWorkoutCompletedSetsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        workoutId: Long,
        workoutLifts: List<GenericWorkoutLift>,
        newWorkoutLiftIndices: Map<Long, Int>,
    ) = transactionScope.execute {
        val workoutLiftPositionByLiftId = mutableMapOf<Long, Int>()
        val delta = programDelta {
            workout(workoutId) {
                workoutLifts.fastForEach { workoutLift ->
                    workoutLiftPositionByLiftId.put(workoutLift.liftId, workoutLift.position)
                    updateLift(
                        workoutLiftId = workoutLift.id,
                        position = Patch.Set(newWorkoutLiftIndices[workoutLift.id]!!)
                    )
                }
            }
        }

        programsRepository.applyDelta(programId, delta)

        if (workoutInProgressRepository.isWorkoutInProgress(workoutId)) {
            programsRepository.getActive()?.let { programMetadata ->
                val updatedInProgressSetResults = liveWorkoutCompletedSetsRepository.getAll()
                    .map { completedSet ->
                        val liftPosition = workoutLiftPositionByLiftId[completedSet.liftId]!!
                        when (completedSet) {
                            is StandardSetResult -> completedSet.copy(liftPosition = liftPosition)
                            is MyoRepSetResult -> completedSet.copy(liftPosition = liftPosition)
                            is LinearProgressionSetResult -> completedSet.copy(liftPosition = liftPosition)
                            else -> throw Exception("${completedSet::class.simpleName} is not defined.")
                        }
                    }

                liveWorkoutCompletedSetsRepository.upsertMany(updatedInProgressSetResults)
            }
        }
    }
}