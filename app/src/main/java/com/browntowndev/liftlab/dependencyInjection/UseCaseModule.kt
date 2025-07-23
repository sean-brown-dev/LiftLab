package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.domain.progression.StandardProgressionFactory
import org.koin.dsl.module

val useCaseModule = module {
    factory { StandardProgressionFactory() }
}