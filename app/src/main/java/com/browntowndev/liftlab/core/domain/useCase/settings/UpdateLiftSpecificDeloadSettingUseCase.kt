package com.browntowndev.liftlab.core.domain.useCase.settings

import com.browntowndev.liftlab.core.domain.extensions.getAllLiftsWithRecalculatedStepSize
import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.repositories.SettingKey
import com.browntowndev.liftlab.core.domain.repositories.SettingsRepository
import com.browntowndev.liftlab.core.domain.repositories.WorkoutLiftsRepository

class UpdateLiftSpecificDeloadSettingUseCase(
    private val workoutLiftsRepository: WorkoutLiftsRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(program: Program, useLiftSpecificDeload: Boolean) {
        val liftsWithNewStepSizes = program.workouts.getAllLiftsWithRecalculatedStepSize(
            deloadToUseInsteadOfLiftLevel = if (useLiftSpecificDeload) null else program.deloadWeek,
        )

        if (liftsWithNewStepSizes.isNotEmpty()) {
            workoutLiftsRepository.updateMany(liftsWithNewStepSizes.values.toList())
            settingsRepository.setSetting(
                SettingKey.LiftSpecificDeload,
                useLiftSpecificDeload
            )
        } else {
            settingsRepository.setSetting(
                SettingKey.LiftSpecificDeload,
                useLiftSpecificDeload
            )
        }

        if (useLiftSpecificDeload) {
            settingsRepository.setSetting(SettingKey.PromptForDeloadWeek, false)
        }
    }
}