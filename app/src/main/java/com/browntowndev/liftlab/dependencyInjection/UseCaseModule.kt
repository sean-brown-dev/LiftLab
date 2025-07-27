package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.domain.useCase.GetWorkoutCompletionSummaryUseCase
import com.browntowndev.liftlab.core.domain.useCase.HydrateLoggingWorkoutWithCompletedSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase
import com.browntowndev.liftlab.core.domain.useCase.progression.CalculateLoggingWorkoutUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single { CalculateLoggingWorkoutUseCase() }
    single { HydrateLoggingWorkoutWithCompletedSetsUseCase() }
    single { HydrateLoggingWorkoutWithPartiallyCompletedSetsUseCase() }
    single { GetWorkoutCompletionSummaryUseCase() }
}