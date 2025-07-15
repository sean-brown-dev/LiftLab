package com.browntowndev.liftlab.core.dependencyInjection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.qualifier.named
import org.koin.dsl.module

val liftLabScopeModule = module {
    single(named("FirestoreSyncScope")) {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
}