package com.browntowndev.liftlab.core.domain.useCase.workoutConfiguration

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.data.common.TransactionScope
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.extensions.convertToCustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.interfaces.GenericWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.CustomWorkoutLift
import com.browntowndev.liftlab.core.domain.models.workout.StandardWorkoutLift
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository

class ConvertWorkoutLiftTypeUseCase(
    private val programsRepository: ProgramsRepository,
    private val transactionScope: TransactionScope,
) {
    suspend operator fun invoke(
        programId: Long,
        workoutLiftToConvert: GenericWorkoutLift,
        enableCustomSets: Boolean
    ) = transactionScope.execute {
            if (enableCustomSets) {
                val standardWorkoutLift = workoutLiftToConvert as? StandardWorkoutLift
                    ?: throw Exception("Lift already has custom lift sets.")

                val customWorkoutLift = standardWorkoutLift.convertToCustomWorkoutLift()
                val delta = programDelta {
                    workout(customWorkoutLift.workoutId) {
                        updateSets(customWorkoutLift.id) {
                            customWorkoutLift.customLiftSets.fastForEach { customSet ->
                                set(customSet)
                            }
                        }
                    }
                }

                programsRepository.applyDelta(programId, delta)
            } else {
                val customWorkoutLift = workoutLiftToConvert as? CustomWorkoutLift
                    ?: throw Exception("Lift does not have custom lift sets to remove.")

                val topCustomLiftSet = customWorkoutLift.customLiftSets.maxByOrNull { it.position }
                val delta = programDelta {
                    workout(customWorkoutLift.workoutId) {
                        updateLift(
                            workoutLiftId = customWorkoutLift.id,
                            repRangeBottom = topCustomLiftSet?.repRangeBottom ?: 8,
                            repRangeTop = topCustomLiftSet?.repRangeTop ?: 10,
                            rpeTarget = topCustomLiftSet?.rpeTarget ?: 8f,
                        ) {
                            removeAllSets()
                        }
                    }
                }
                programsRepository.applyDelta(programId, delta)
            }
        }
}