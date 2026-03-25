package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.data.remote.client.ProgramGenerationAiClient
import com.browntowndev.liftlab.core.domain.ai.AiClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val aiModule = module {
    singleOf<AiClient>(::ProgramGenerationAiClient)
}