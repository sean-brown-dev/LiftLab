package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.data.repositories.CustomLiftSetsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutLiftsRepositoryImpl
import com.browntowndev.liftlab.core.data.repositories.WorkoutsRepositoryImpl
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.Workout
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class ProgramCloner {
    companion object {
        suspend fun clone(
            programsRepository: ProgramsRepository,
            workoutsRepositoryImpl: WorkoutsRepositoryImpl,
            workoutLiftsRepositoryImpl: WorkoutLiftsRepositoryImpl,
            setsRepository: CustomLiftSetsRepositoryImpl,
            program: Program
        ) {
            val clonedProgram = Program(
                name = program.name,
                isActive = program.isActive,
                deloadWeek = program.deloadWeek,
            )

            val programId = programsRepository.insert(clonedProgram)
            clonedProgram.workouts.fastForEach { workout ->
                clone(workoutsRepositoryImpl, workoutLiftsRepositoryImpl, setsRepository, programId, workout)
            }
        }

        suspend fun clone(
            workoutsRepositoryImpl: WorkoutsRepositoryImpl,
            workoutLiftsRepositoryImpl: WorkoutLiftsRepositoryImpl,
            setsRepository: CustomLiftSetsRepositoryImpl,
            programId: Long,
            workout: Workout
        ) {
            val workoutClone = Workout(
                programId = programId,
                name = workout.name,
                position = workout.position,
                lifts = listOf(),
            )

            val workoutId = workoutsRepositoryImpl.insert(workoutClone)
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

                val workoutLiftId = workoutLiftsRepositoryImpl.insert(clonedLift)
                if (lift is CustomWorkoutLift) {
                    clone(setsRepository, workoutLiftId, lift.customLiftSets)
                }
            }
        }

        suspend fun clone(setsRepository: CustomLiftSetsRepositoryImpl, workoutLiftId: Long, setsToClone: List<GenericLiftSet>) {
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
}