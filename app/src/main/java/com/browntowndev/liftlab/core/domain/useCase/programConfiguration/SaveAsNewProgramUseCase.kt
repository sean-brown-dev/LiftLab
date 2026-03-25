package com.browntowndev.liftlab.core.domain.useCase.programConfiguration

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.DropSet
import com.browntowndev.liftlab.core.domain.models.workout.MyoRepSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardSet
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class SaveAsNewProgramUseCase(
    private val programsRepository: ProgramsRepository
) {
    /**
     * Clones [sourceProgram] into a brand new program.
     *
     * @param sourceProgram The program to clone.
     * @param newName Optional new name; defaults to "<old> (Copy)".
     * @param isActive Whether the new program is active (defaults to false).
     * @return The new program's id.
     */
    suspend operator fun invoke(
        sourceProgram: Program,
        newName: String = "${sourceProgram.name} (Copy)",
        isActive: Boolean = false
    ): Long {
        // 1) Create the new parent row (no children yet)
        val newProgramId = programsRepository.insert(
            Program(
                id = 0L,
                name = newName,
                isActive = isActive,
                currentMesocycle = sourceProgram.currentMesocycle,
                currentMicrocycle = sourceProgram.currentMicrocycle,
                currentMicrocyclePosition = sourceProgram.currentMicrocyclePosition,
                workouts = emptyList() // children will be added via delta below
            )
        )

        // 2) Build a delta that INSERTS all workouts/lifts/sets under the new program
        val delta = programDelta {
            sourceProgram.workouts.fastForEach { sourceWorkout ->
                workout(
                    insertWorkout = sourceWorkout.copy(
                        id = 0L,
                        programId = newProgramId,
                        lifts = sourceWorkout.lifts.fastMap { sourceWorkoutLift ->
                            when (sourceWorkoutLift) {
                                is StandardWorkoutLift -> {
                                    sourceWorkoutLift.copy( id = 0L, workoutId = 0L)
                                }
                                is CustomWorkoutLift -> {
                                    sourceWorkoutLift.copy(
                                        id = 0L,
                                        workoutId = 0L,
                                        customLiftSets = sourceWorkoutLift.customLiftSets.fastMap { sourceSet ->
                                            when (sourceSet) {
                                                is StandardSet -> sourceSet.copy(id = 0L, workoutLiftId = 0L)
                                                is DropSet     -> sourceSet.copy(id = 0L, workoutLiftId = 0L)
                                                is MyoRepSet   -> sourceSet.copy(id = 0L, workoutLiftId = 0L)
                                                else -> error("Unsupported set type ${sourceSet::class.simpleName}")
                                            }
                                        }
                                    )
                                }

                                else -> error("Unsupported workout lift type ${sourceWorkoutLift::class.simpleName}")
                            }
                        }
                    )
                )
            }
        }

        programsRepository.applyDelta(newProgramId, delta)

        return newProgramId
    }
}
