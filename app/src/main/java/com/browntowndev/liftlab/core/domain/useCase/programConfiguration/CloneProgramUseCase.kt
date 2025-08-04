package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutsRepository

class CloneProgramUseCase {
    suspend operator fun invoke(
        programsRepository: ProgramsRepository,
        workoutsRepository: WorkoutsRepository,
        workoutLiftsRepository: WorkoutLiftsRepository,
        setsRepository: CustomLiftSetsRepository,
        program: Program
    ) {
        val clonedProgram = Program(
            name = program.name,
            isActive = program.isActive,
            deloadWeek = program.deloadWeek,
        )

        val programId = programsRepository.insert(clonedProgram)
        clonedProgram.workouts.fastForEach { workout ->
            cloneWorkout(
                workoutsRepository,
                workoutLiftsRepository,
                setsRepository,
                programId,
                workout
            )
        }
    }

    private suspend fun cloneWorkout(
        workoutsRepository: WorkoutsRepository,
        workoutLiftsRepository: WorkoutLiftsRepository,
        setsRepository: CustomLiftSetsRepository,
        programId: Long,
        workout: Workout
    ) {
        val workoutClone = Workout(
            programId = programId,
            name = workout.name,
            position = workout.position,
            lifts = listOf(),
        )

        val workoutId = workoutsRepository.insert(workoutClone)
        workout.lifts.fastForEach { lift ->
            val clonedLift = when (lift) {
                is StandardWorkoutLift -> StandardWorkoutLift(
                    workoutId = workoutId,
                    liftId = lift.liftId,
                    liftName = lift.liftName,
                    liftMovementPattern = lift.liftMovementPattern,
                    liftVolumeTypes = lift.liftVolumeTypes,
                    liftSecondaryVolumeTypes = lift.liftSecondaryVolumeTypes,
                    position = lift.position,
                    setCount = lift.setCount,
                    progressionScheme = lift.progressionScheme,
                    incrementOverride = lift.incrementOverride,
                    restTime = lift.restTime,
                    restTimerEnabled = lift.restTimerEnabled,
                    deloadWeek = lift.deloadWeek,
                    liftNote = lift.liftNote,
                    rpeTarget = lift.rpeTarget,
                    repRangeBottom = lift.repRangeBottom,
                    repRangeTop = lift.repRangeTop,
                    stepSize = lift.stepSize
                )

                is CustomWorkoutLift -> CustomWorkoutLift(
                    workoutId = workoutId,
                    liftId = lift.liftId,
                    liftName = lift.liftName,
                    liftMovementPattern = lift.liftMovementPattern,
                    liftVolumeTypes = lift.liftVolumeTypes,
                    liftSecondaryVolumeTypes = lift.liftSecondaryVolumeTypes,
                    position = lift.position,
                    setCount = lift.setCount,
                    progressionScheme = lift.progressionScheme,
                    incrementOverride = lift.incrementOverride,
                    restTime = lift.restTime,
                    restTimerEnabled = lift.restTimerEnabled,
                    deloadWeek = lift.deloadWeek,
                    liftNote = lift.liftNote,
                    customLiftSets = listOf()
                )

                else -> throw Exception("Type ${lift::class.simpleName} is not defined.")
            }

            val workoutLiftId = workoutLiftsRepository.insert(clonedLift)
            if (lift is CustomWorkoutLift) {
                cloneCustomSets(setsRepository, workoutLiftId, lift.customLiftSets)
            }
        }
    }

    private suspend fun cloneCustomSets(
        setsRepository: CustomLiftSetsRepository,
        workoutLiftId: Long,
        setsToClone: List<GenericLiftSet>
    ) {
        setsToClone.fastForEach { set ->
            val clonedSet = when (set) {
                is StandardSet -> StandardSet(
                    workoutLiftId = workoutLiftId,
                    position = set.position,
                    repRangeTop = set.repRangeTop,
                    repRangeBottom = set.repRangeBottom,
                    rpeTarget = set.rpeTarget,
                )

                is DropSet -> DropSet(
                    workoutLiftId = workoutLiftId,
                    position = set.position,
                    repRangeTop = set.repRangeTop,
                    repRangeBottom = set.repRangeBottom,
                    rpeTarget = set.rpeTarget,
                    dropPercentage = set.dropPercentage,
                )

                is MyoRepSet -> MyoRepSet(
                    workoutLiftId = workoutLiftId,
                    position = set.position,
                    repRangeTop = set.repRangeTop,
                    repRangeBottom = set.repRangeBottom,
                    rpeTarget = set.rpeTarget,
                    repFloor = set.repFloor,
                    setGoal = set.setGoal,
                    setMatching = set.setMatching,
                    maxSets = set.maxSets,
                )

                else -> throw Exception("Type ${set::class.simpleName} is not defined.")
            }

            setsRepository.insert(clonedSet)
        }
    }
}