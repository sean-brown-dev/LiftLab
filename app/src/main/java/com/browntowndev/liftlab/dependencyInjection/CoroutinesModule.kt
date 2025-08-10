package com.browntowndev.liftlab.dependencyInjection

import com.browntowndev.liftlab.core.coroutines.AppDispatchers
import com.browntowndev.liftlab.core.coroutines.RealDispatchers
import org.koin.dsl.module

val coroutinesModule = module {
    single<AppDispatchers> {
        RealDispatchers()
    }
}