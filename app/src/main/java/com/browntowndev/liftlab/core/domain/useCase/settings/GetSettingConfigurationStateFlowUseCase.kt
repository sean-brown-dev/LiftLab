package com.browntowndev.liftlab.core.domain.useCase.settings

import com.browntowndev.liftlab.core.domain.models.programConfiguration.Program
import com.browntowndev.liftlab.core.domain.models.settings.SettingsConfigurationState
import com.browntowndev.liftlab.core.domain.repositories.ProgramsRepository
import com.browntowndev.liftlab.core.domain.common.SettingKey
import com.browntowndev.liftlab.core.domain.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class GetSettingConfigurationStateFlowUseCase(
    private val programsRepository: ProgramsRepository,
    private val settingsRepository: SettingsRepository,
) {
    private data class IntermediateState(
        val activeProgram: Program?,
        val defaultIncrement: Float,
        val defaultRestTime: Duration,
        val useAllLiftDataForRecommendations: Boolean
    )

    operator fun invoke(): Flow<SettingsConfigurationState> {
        val activeProgramFlow = programsRepository.getActiveProgramFlow()
        val defaultIncrementFlow = settingsRepository.getSettingFlow(SettingKey.Increment)
        val defaultRestTimeFlow = settingsRepository.getSettingFlow(SettingKey.RestTime)
        val useAllLiftDataForRecommendationsFlow = settingsRepository.getSettingFlow(SettingKey.UseAllLiftDataForRecommendations)
        val useOnlyResultsFromLiftInSamePositionFlow = settingsRepository.getSettingFlow(SettingKey.UseOnlyResultsFromLiftInSamePosition)
        val liftSpecificDeloadingFlow = settingsRepository.getSettingFlow(SettingKey.LiftSpecificDeload)
        val promptOnDeloadStartFlow = settingsRepository.getSettingFlow(SettingKey.PromptForDeloadWeek)

        val defaultRestTimeDurationFlow = defaultRestTimeFlow.map { defaultRestTime ->
            defaultRestTime.toDuration(DurationUnit.MILLISECONDS)
        }

        val firstFourFlow = combine(
            activeProgramFlow,
            defaultIncrementFlow,
            defaultRestTimeDurationFlow,
            useAllLiftDataForRecommendationsFlow,
        ) { activeProgram, defaultIncrement, defaultRestTime, useAllLiftDataForRecommendations ->
            IntermediateState(
                activeProgram = activeProgram,
                defaultIncrement = defaultIncrement,
                defaultRestTime = defaultRestTime,
                useAllLiftDataForRecommendations = useAllLiftDataForRecommendations
            )
        }

        return combine(
            firstFourFlow,
            useOnlyResultsFromLiftInSamePositionFlow,
            liftSpecificDeloadingFlow,
            promptOnDeloadStartFlow
        ) { intermediateState, useOnlyResultsFromLiftInSamePosition, liftSpecificDeloading, promptOnDeloadStart ->
            SettingsConfigurationState(
                activeProgram = intermediateState.activeProgram,
                defaultIncrement = intermediateState.defaultIncrement,
                defaultRestTime = intermediateState.defaultRestTime,
                useAllLiftDataForRecommendations = intermediateState.useAllLiftDataForRecommendations,
                useOnlyResultsFromLiftInSamePosition = useOnlyResultsFromLiftInSamePosition,
                liftSpecificDeloading = liftSpecificDeloading,
                promptOnDeloadStart = promptOnDeloadStart
            )
        }
    }
}