package com.browntowndev.liftlab.core.common

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.persistence.dtos.CustomWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.DropSetDto
import com.browntowndev.liftlab.core.persistence.dtos.MyoRepSetDto
import com.browntowndev.liftlab.core.persistence.dtos.ProgramDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardSetDto
import com.browntowndev.liftlab.core.persistence.dtos.StandardWorkoutLiftDto
import com.browntowndev.liftlab.core.persistence.dtos.WorkoutDto
import com.browntowndev.liftlab.core.persistence.dtos.interfaces.GenericLiftSet
import com.browntowndev.liftlab.core.persistence.repositories.CustomLiftSetsRepository
import com.browntowndev.liftlab.core.persistence.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutLiftsRepository
import com.browntowndev.liftlab.core.persistence.repositories.WorkoutsRepository

class ProgramCloner {
    companion object {
        suspend fun clone(
            programsRepository: ProgramsRepository,
            workoutsRepository: WorkoutsRepository,
            workoutLiftsRepository: WorkoutLiftsRepository,
            setsRepository: CustomLiftSetsRepository,
            program: ProgramDto
        ) {
            val clonedProgram = ProgramDto(
                name = program.name,
                isActive = program.isActive,
                deloadWeek = program.deloadWeek,
            )

            val programId = programsRepository.insert(clonedProgram)
            clonedProgram.workouts.fastForEach { workout ->
                clone(workoutsRepository, workoutLiftsRepository, setsRepository, programId, workout)
            }
        }

        suspend fun clone(
            workoutsRepository: WorkoutsRepository,
            workoutLiftsRepository: WorkoutLiftsRepository,
            setsRepository: CustomLiftSetsRepository,
            programId: Long,
            workout: WorkoutDto
        ) {
            val workoutClone = WorkoutDto(
                programId = programId,
                name = workout.name,
                position = workout.position,
                lifts = listOf(),
            )

            val workoutId = workoutsRepository.insert(workoutClone)
            workout.lifts.fastForEach { lift ->
                val clonedLift = when (lift) {
                    is StandardWorkoutLiftDto -> StandardWorkoutLiftDto(
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
                    is CustomWorkoutLiftDto -> CustomWorkoutLiftDto(
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
                if (lift is CustomWorkoutLiftDto) {
                    clone(setsRepository, workoutLiftId, lift.customLiftSets)
                }
            }
        }

        suspend fun clone(setsRepository: CustomLiftSetsRepository, workoutLiftId: Long, setsToClone: List<GenericLiftSet>) {
            setsToClone.fastForEach { set ->
                val clonedSet = when (set) {
                    is StandardSetDto -> StandardSetDto(
                        workoutLiftId = workoutLiftId,
                        position = set.position,
                        repRangeTop = set.repRangeTop,
                        repRangeBottom = set.repRangeBottom,
                        rpeTarget = set.rpeTarget,
                    )
                    is DropSetDto -> DropSetDto(
                        workoutLiftId = workoutLiftId,
                        position = set.position,
                        repRangeTop = set.repRangeTop,
                        repRangeBottom = set.repRangeBottom,
                        rpeTarget = set.rpeTarget,
                        dropPercentage = set.dropPercentage,
                    )
                    is MyoRepSetDto -> MyoRepSetDto(
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