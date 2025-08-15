package com.browntowndev.liftlab.core.domain.useCase.settings

import androidx.compose.ui.util.fastForEach
import com.browntowndev.liftlab.core.common.Patch
import com.browntowndev.liftlab.core.domain.delta.programDelta
import com.browntowndev.liftlab.core.domain.extensions.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.repositories.SettingKey
import com.browntowndev.liftlab.core.domain.repositories.SettingsRepository

class UpdateLiftSpecificDeloadSettingUseCase(
    private val programsRepository: ProgramsRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(program: Program, useLiftSpecificDeload: Boolean) {
        val liftsWithNewStepSizes = program.workouts.getAllLiftsWithRecalculatedStepSize(
            deloadToUseInsteadOfLiftLevel = if (useLiftSpecificDeload) null else program.deloadWeek,
        )

        if (liftsWithNewStepSizes.isNotEmpty()) {
            val delta = programDelta {
                liftsWithNewStepSizes.forEach { workoutLiftWithNewSteps ->
                    val workoutId = workoutLiftWithNewSteps.key
                    val liftsWithNewSteps = workoutLiftWithNewSteps.value
                    workout(workoutId) {
                        liftsWithNewSteps.fastForEach { workoutLift ->
                            updateLift(workoutLiftId = workoutLift.id, stepSize = Patch.Set(workoutLift.stepSize))
                        }
                    }
                }
            }
            programsRepository.applyDelta(program.id, delta)
        }

        settingsRepository.setSetting(
            SettingKey.LiftSpecificDeload,
            useLiftSpecificDeload
        )

        if (useLiftSpecificDeload) {
            settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false)
        }
    }
}